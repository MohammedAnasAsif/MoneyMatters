package io.github.mohammedanasasif.moneymatters.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import io.github.mohammedanasasif.moneymatters.MoneyMattersApp
import io.github.mohammedanasasif.moneymatters.data.TxnSource
import io.github.mohammedanasasif.moneymatters.parse.UpiParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Logs UPI debits from incoming bank SMS. This covers every UPI app, since the bank always texts. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Long SMS arrive in parts; stitch them back together per sender.
        val bySender = messages.filterNotNull().groupBy { it.originatingAddress.orEmpty() }
        val repository = (context.applicationContext as MoneyMattersApp).repository

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((sender, parts) in bySender) {
                    if (!UpiParser.isBankSender(sender)) continue
                    val body = parts.joinToString("") { it.messageBody.orEmpty() }
                    val parsed = UpiParser.parse(body) ?: continue
                    repository.addCaptured(parsed, parts.first().timestampMillis, TxnSource.SMS, body)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
