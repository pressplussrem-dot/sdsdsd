package com.smartcalc.ai.data.repository

import com.smartcalc.ai.data.remote.SolveRequestDto
import com.smartcalc.ai.data.remote.SolverApi
import com.smartcalc.ai.util.ImageProcessor
import com.smartcalc.ai.util.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

class SolverRepository(
    private val api: SolverApi,
    private val networkMonitor: NetworkMonitor,
    private val imageProcessor: ImageProcessor,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val REQUEST_TIMEOUT_MS = 60_000L
        private const val MIN_CONFIDENCE = 0.55
    }

    suspend fun solve(imageFile: File): SolveResult = withContext(io) {
        if (!networkMonitor.isOnline()) {
            return@withContext SolveResult.Failure(FailureReason.NO_INTERNET)
        }

        val encoded = imageProcessor.toBase64Jpeg(imageFile)
            ?: return@withContext SolveResult.Failure(FailureReason.INVALID_IMAGE)

        try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                api.solve(SolveRequestDto(imageBase64 = encoded))
            }

            if (!response.isSuccessful) {
                return@withContext when (response.code()) {
                    in 500..599 -> SolveResult.Failure(FailureReason.BACKEND_UNAVAILABLE)
                    408, 504 -> SolveResult.Failure(FailureReason.TIMEOUT)
                    413, 415, 422 -> SolveResult.Failure(FailureReason.INVALID_IMAGE)
                    else -> SolveResult.Failure(FailureReason.AI_ERROR)
                }
            }

            val body = response.body()
                ?: return@withContext SolveResult.Failure(FailureReason.INVALID_RESPONSE)

            when (body.status?.lowercase()) {
                "ok" -> {
                    val problem = body.problem?.trim().orEmpty()
                    val solution = body.solution?.trim().orEmpty()
                    val confidence = body.confidence
                    val lowConfidence = confidence != null && confidence < MIN_CONFIDENCE
                    if (problem.isEmpty() || solution.isEmpty() || lowConfidence) {
                        // Do not guess. Ask the user for a better photo instead.
                        SolveResult.Unclear
                    } else {
                        SolveResult.Success(
                            problem = problem,
                            steps = body.steps.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
                            solution = solution,
                            confidence = confidence
                        )
                    }
                }
                "unclear" -> SolveResult.Unclear
                "error" -> SolveResult.Failure(FailureReason.AI_ERROR)
                else -> SolveResult.Failure(FailureReason.INVALID_RESPONSE)
            }
        } catch (e: TimeoutCancellationException) {
            SolveResult.Failure(FailureReason.TIMEOUT)
        } catch (e: SocketTimeoutException) {
            SolveResult.Failure(FailureReason.TIMEOUT)
        } catch (e: IOException) {
            SolveResult.Failure(FailureReason.BACKEND_UNAVAILABLE)
        } catch (e: Exception) {
            SolveResult.Failure(FailureReason.INVALID_RESPONSE)
        }
    }
}
