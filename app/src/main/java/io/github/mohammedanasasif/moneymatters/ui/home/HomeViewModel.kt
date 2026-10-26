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

    private val detailId = MutableStateFlow<String?>(null)
    private val detailRange = MutableStateFlow(Range.M1)

    val home: StateFlow<HomeState?> =
        repository.all
            .map { buildHomeState(it, System.currentTimeMillis()) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val detail: StateFlow<DetailState?> =
        combine(repository.all, detailId, detailRange) { all, id, range ->
            id?.let { buildDetailState(all, it, range, System.currentTimeMillis()) }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val needsReview: StateFlow<List<Transaction>> =
        repository.all
            .map { list -> list.filter { it.status == TxnStatus.NEEDS_REVIEW } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openDetail(id: String) {
        detailRange.value = Range.M1
        detailId.value = id
    }

    fun closeDetail() {
        detailId.value = null
    }

    fun setDetailRange(value: Range) {
        detailRange.value = value
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
