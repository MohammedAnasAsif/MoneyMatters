package io.github.mohammedanasasif.moneymatters.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.mohammedanasasif.moneymatters.MoneyMattersApp
import io.github.mohammedanasasif.moneymatters.data.TxnSource
import io.github.mohammedanasasif.moneymatters.parse.UpiParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Backup capture from "payment successful" notifications of UPI apps, for when the bank SMS
 * is late or SMS access is off. Only the apps listed below are read; everything else is ignored.
 */
class UpiNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in UPI_APPS) return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val combined = listOf(title, text).filter { it.isNotBlank() }.joinToString(". ")
        val parsed = UpiParser.parse(combined, requireUpiMarker = false) ?: return

        val repository = (applicationContext as MoneyMattersApp).repository
        scope.launch { repository.addCaptured(parsed, sbn.postTime, TxnSource.NOTIFICATION, combined) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        val UPI_APPS = setOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp", // BHIM
            "com.dreamplug.androidapp", // CRED
            "in.amazon.mShop.android.shopping", // Amazon Pay
            "com.sbi.upi", // BHIM SBI Pay
            "com.csam.icici.bank.imobile",
            "com.snapwork.hdfc",
            "com.axis.mobile",
            "com.msf.kbank.mobile", // Kotak
            "money.super.payments", // super.money
            "com.mobikwik_new",
        )
    }
}
