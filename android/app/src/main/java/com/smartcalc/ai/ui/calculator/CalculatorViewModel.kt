package com.smartcalc.ai.ui.calculator

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartcalc.ai.R
import com.smartcalc.ai.SmartCalcApplication
import com.smartcalc.ai.calculator.CalcErrorType
import com.smartcalc.ai.calculator.CalculationException
import com.smartcalc.ai.calculator.CalculatorInput
import com.smartcalc.ai.calculator.ExpressionEvaluator
import com.smartcalc.ai.data.repository.HistoryRepository
import com.smartcalc.ai.util.CalculatorHandoff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val expression: String = "",
    val display: String = "0",
    @StringRes val errorRes: Int? = null,
    val isFinalResult: Boolean = false
)

class CalculatorViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        // Pick up an expression the user tapped in the history screen.
        CalculatorHandoff.consume()?.let { expression ->
            setExpression(expression)
        }
    }

    fun refreshHandoff() {
        CalculatorHandoff.consume()?.let { setExpression(it) }
    }

    fun onDigit(digit: Char) = update { current ->
        val base = if (current.isFinalResult) "" else current.expression
        CalculatorInput.digit(base, digit)
    }

    fun onDecimal() = update { current ->
        val base = if (current.isFinalResult) "" else current.expression
        CalculatorInput.decimal(base)
    }

    fun onOperator(op: Char) = update { current ->
        // Continue calculating with the previous result.
        val base = if (current.isFinalResult) current.display else current.expression
        CalculatorInput.operator(base, op)
    }

    fun onParenthesis() = update { current ->
        val base = if (current.isFinalResult) "" else current.expression
        CalculatorInput.parenthesis(base)
    }

    fun onPercent() = update { current ->
        val base = if (current.isFinalResult) current.display else current.expression
        CalculatorInput.percent(base)
    }

    fun onToggleSign() = update { current ->
        val base = if (current.isFinalResult) current.display else current.expression
        CalculatorInput.toggleSign(base)
    }

    fun onBackspace() = update { current ->
        val base = if (current.isFinalResult) current.display else current.expression
        CalculatorInput.backspace(base)
    }

    fun onClear() {
        _uiState.value = CalculatorUiState()
    }

    fun onEquals() {
        val expression = _uiState.value.expression
        if (expression.isBlank()) return

        try {
            val result = ExpressionEvaluator.evaluateToString(expression)
            _uiState.value = CalculatorUiState(
                expression = expression,
                display = result,
                errorRes = null,
                isFinalResult = true
            )
            viewModelScope.launch {
                runCatching { historyRepository.saveCalculation(expression, result) }
            }
        } catch (e: CalculationException) {
            _uiState.value = _uiState.value.copy(
                errorRes = messageFor(e.type),
                isFinalResult = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorRes = R.string.error_invalid_expression,
                isFinalResult = false
            )
        }
    }

    private fun setExpression(expression: String) {
        val preview = ExpressionEvaluator.tryEvaluateToString(expression)
        _uiState.value = CalculatorUiState(
            expression = expression,
            display = preview ?: "0",
            errorRes = null,
            isFinalResult = false
        )
    }

    private inline fun update(transform: (CalculatorUiState) -> String) {
        val current = _uiState.value
        val expression = transform(current)
        val preview = ExpressionEvaluator.tryEvaluateToString(expression)
        _uiState.value = CalculatorUiState(
            expression = expression,
            display = preview ?: if (expression.isEmpty()) "0" else current.display,
            errorRes = null,
            isFinalResult = false
        )
    }

    @StringRes
    private fun messageFor(type: CalcErrorType): Int = when (type) {
        CalcErrorType.DIVISION_BY_ZERO -> R.string.error_division_by_zero
        CalcErrorType.NUMBER_TOO_LARGE -> R.string.error_number_too_large
        CalcErrorType.EMPTY_EXPRESSION -> R.string.error_invalid_expression
        CalcErrorType.INVALID_EXPRESSION -> R.string.error_invalid_expression
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as SmartCalcApplication
                CalculatorViewModel(app.container.historyRepository)
            }
        }
    }
}
