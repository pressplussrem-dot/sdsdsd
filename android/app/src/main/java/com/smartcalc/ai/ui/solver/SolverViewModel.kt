package com.smartcalc.ai.ui.solver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartcalc.ai.R
import com.smartcalc.ai.SmartCalcApplication
import com.smartcalc.ai.data.repository.FailureReason
import com.smartcalc.ai.data.repository.HistoryRepository
import com.smartcalc.ai.data.repository.SolveResult
import com.smartcalc.ai.data.repository.SolverRepository
import com.smartcalc.ai.util.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SolverViewModel(
    private val solverRepository: SolverRepository,
    private val historyRepository: HistoryRepository,
    private val imageProcessor: ImageProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow<SolverUiState>(SolverUiState.AwaitingImage)
    val uiState: StateFlow<SolverUiState> = _uiState.asStateFlow()

    private val _savedEvent = MutableStateFlow(false)
    val savedEvent: StateFlow<Boolean> = _savedEvent.asStateFlow()

    fun onImageReady(file: File) {
        viewModelScope.launch {
            val blurry = withContext(Dispatchers.Default) { imageProcessor.looksTooBlurry(file) }
            imageProcessor.clearOldCaptures(keep = file)
            _uiState.value = SolverUiState.ImageReady(
                file = file,
                warningRes = if (blurry) R.string.error_image_too_blurry else null
            )
        }
    }

    fun onImageError(messageRes: Int) {
        _uiState.value = SolverUiState.Error(messageRes = messageRes)
    }

    fun retakeImage() {
        _uiState.value = SolverUiState.AwaitingImage
    }

    fun solve() {
        val file = currentFile() ?: return
        _uiState.value = SolverUiState.Solving(file)

        viewModelScope.launch {
            when (val result = solverRepository.solve(file)) {
                is SolveResult.Success -> {
                    _uiState.value = SolverUiState.Solved(
                        file = file,
                        problem = result.problem,
                        steps = result.steps,
                        solution = result.solution
                    )
                }
                SolveResult.Unclear -> {
                    _uiState.value = SolverUiState.Unclear(file)
                }
                is SolveResult.Failure -> {
                    _uiState.value = SolverUiState.Error(
                        messageRes = messageFor(result.reason),
                        file = file,
                        canRetrySolve = true
                    )
                }
            }
        }
    }

    fun saveToHistory() {
        val state = _uiState.value as? SolverUiState.Solved ?: return
        if (state.saved) return

        viewModelScope.launch {
            val imagePath = withContext(Dispatchers.IO) {
                imageProcessor.persistForHistory(state.file)
            }
            runCatching {
                historyRepository.saveAiSolution(
                    problem = state.problem,
                    solution = state.solution,
                    steps = state.steps,
                    imagePath = imagePath
                )
            }.onSuccess {
                _uiState.value = state.copy(saved = true)
                _savedEvent.value = true
            }
        }
    }

    fun consumeSavedEvent() {
        _savedEvent.value = false
    }

    fun startNewTask() {
        _uiState.value = SolverUiState.AwaitingImage
    }

    private fun currentFile(): File? = when (val state = _uiState.value) {
        is SolverUiState.ImageReady -> state.file
        is SolverUiState.Solving -> state.file
        is SolverUiState.Solved -> state.file
        is SolverUiState.Unclear -> state.file
        is SolverUiState.Error -> state.file
        SolverUiState.AwaitingImage -> null
    }

    private fun messageFor(reason: FailureReason): Int = when (reason) {
        FailureReason.NO_INTERNET -> R.string.error_need_internet_for_ai
        FailureReason.BACKEND_UNAVAILABLE -> R.string.error_backend_unavailable
        FailureReason.TIMEOUT -> R.string.error_timeout
        FailureReason.AI_ERROR -> R.string.error_ai
        FailureReason.INVALID_RESPONSE -> R.string.error_invalid_response
        FailureReason.INVALID_IMAGE -> R.string.error_image_invalid
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as SmartCalcApplication
                SolverViewModel(
                    solverRepository = app.container.solverRepository,
                    historyRepository = app.container.historyRepository,
                    imageProcessor = app.container.imageProcessor
                )
            }
        }
    }
}
