package io.github.mohammedanasasif.moneymatters.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mohammedanasasif.moneymatters.MoneyMattersApp
import io.github.mohammedanasasif.moneymatters.data.Transaction
import io.github.mohammedanasasif.moneymatters.data.TxnStatus
import io.github.mohammedanasasif.moneymatters.data.TxnType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as MoneyMattersApp).repository

    private val range = MutableStateFlow(Range.M1)
    private val mode = MutableStateFlow(ListMode.RECENT)
    private val filter = MutableStateFlow<Filter?>(null)

    val state: StateFlow<HomeState?> =
        combine(repository.all, range, mode, filter) { all, r, m, f ->
            buildHomeState(all, r, m, f, System.currentTimeMillis())
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val needsReview: StateFlow<List<Transaction>> =
        repository.all
            .map { list -> list.filter { it.status == TxnStatus.NEEDS_REVIEW } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setRange(value: Range) {
        range.value = value
    }

    fun setMode(value: ListMode) {
        mode.value = value
        filter.value = null
    }

    fun setFilter(value: Filter?) {
        filter.value = value
    }

    fun addManual(amountPaise: Long, type: TxnType, payee: String?, category: String?, timestamp: Long) {
        viewModelScope.launch { repository.addManual(amountPaise, type, payee, category, timestamp) }
    }

    fun update(transaction: Transaction) {
        viewModelScope.launch { repository.update(transaction) }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { repository.delete(transaction) }
    }

    fun confirm(transaction: Transaction) {
        update(transaction.copy(status = TxnStatus.CONFIRMED))
    }
}
