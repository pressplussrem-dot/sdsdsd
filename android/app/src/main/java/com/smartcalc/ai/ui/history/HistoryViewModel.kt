package com.smartcalc.ai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartcalc.ai.SmartCalcApplication
import com.smartcalc.ai.data.local.HistoryEntity
import com.smartcalc.ai.data.local.HistoryType
import com.smartcalc.ai.data.repository.HistoryRepository
import com.smartcalc.ai.util.CalculatorHandoff
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter { ALL, CALCULATOR, AI }

class HistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.ALL)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<HistoryEntity>> = _filter
        .flatMapLatest { filter ->
            when (filter) {
                HistoryFilter.ALL -> repository.observeAll()
                HistoryFilter.CALCULATOR -> repository.observeByType(HistoryType.CALCULATOR)
                HistoryFilter.AI -> repository.observeByType(HistoryType.AI)
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun delete(id: Long) {
        viewModelScope.launch { runCatching { repository.delete(id) } }
    }

    fun deleteAll() {
        viewModelScope.launch { runCatching { repository.deleteAll() } }
    }

    fun reuse(entity: HistoryEntity) {
        if (entity.type == HistoryType.CALCULATOR) {
            CalculatorHandoff.reuse(entity.expression)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as SmartCalcApplication
                HistoryViewModel(app.container.historyRepository)
            }
        }
    }
}
