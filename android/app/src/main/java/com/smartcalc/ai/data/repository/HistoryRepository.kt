package com.smartcalc.ai.data.repository

import com.smartcalc.ai.data.local.HistoryDao
import com.smartcalc.ai.data.local.HistoryEntity
import com.smartcalc.ai.data.local.HistoryType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class HistoryRepository(
    private val dao: HistoryDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()

    fun observeByType(type: HistoryType): Flow<List<HistoryEntity>> = dao.observeByType(type)

    suspend fun saveCalculation(expression: String, result: String): Long = withContext(io) {
        dao.insert(
            HistoryEntity(
                type = HistoryType.CALCULATOR,
                expression = expression,
                result = result
            )
        )
    }

    suspend fun saveAiSolution(
        problem: String,
        solution: String,
        steps: List<String>,
        imagePath: String?
    ): Long = withContext(io) {
        dao.insert(
            HistoryEntity(
                type = HistoryType.AI,
                expression = problem,
                result = solution,
                steps = steps.joinToString("\n"),
                imagePath = imagePath
            )
        )
    }

    suspend fun delete(id: Long) = withContext(io) {
        dao.imagePathFor(id)?.let { path ->
            runCatching { File(path).delete() }
        }
        dao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(io) {
        dao.allImagePaths().forEach { path ->
            runCatching { File(path).delete() }
        }
        dao.deleteAll()
    }
}
