package io.github.mohammedanasasif.moneymatters

import android.app.Application
import io.github.mohammedanasasif.moneymatters.data.AppDatabase
import io.github.mohammedanasasif.moneymatters.data.Prefs
import io.github.mohammedanasasif.moneymatters.data.TransactionRepository

class MoneyMattersApp : Application() {
    val prefs by lazy { Prefs(this) }
    val repository by lazy { TransactionRepository(AppDatabase.get(this).transactions(), prefs) }
}
