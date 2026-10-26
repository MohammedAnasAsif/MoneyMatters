package io.github.mohammedanasasif.moneymatters

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.github.mohammedanasasif.moneymatters.capture.SmsImporter
import io.github.mohammedanasasif.moneymatters.data.AppDatabase
import io.github.mohammedanasasif.moneymatters.data.Prefs
import io.github.mohammedanasasif.moneymatters.data.TransactionRepository
import io.github.mohammedanasasif.moneymatters.parse.UpiParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MoneyMattersApp : Application() {
    val prefs by lazy { Prefs(this) }
    val repository by lazy { TransactionRepository(AppDatabase.get(this).transactions(), prefs) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { upgradeStoredData() }
    }

    /** After an update with parser fixes: correct stored payments and pick up SMS that were skipped before. */
    private suspend fun upgradeStoredData() {
        if (prefs.parserVersion >= UpiParser.VERSION) return
        repository.reparseCaptured()
        val canReadSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (canReadSms) SmsImporter.import(this, repository)
        prefs.parserVersion = UpiParser.VERSION
    }
}
