package io.github.mohammedanasasif.moneymatters.capture

import android.content.Context
import android.provider.Telephony
import io.github.mohammedanasasif.moneymatters.data.TransactionRepository
import io.github.mohammedanasasif.moneymatters.data.TxnSource
import io.github.mohammedanasasif.moneymatters.parse.UpiParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One-time backfill from the SMS inbox, so a new install isn't empty. */
object SmsImporter {
    suspend fun import(context: Context, repository: TransactionRepository, days: Int = 90): Int =
        withContext(Dispatchers.IO) {
            val since = System.currentTimeMillis() - days * 86_400_000L
            var added = 0
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(since.toString()),
                "${Telephony.Sms.DATE} ASC",
            )?.use { cursor ->
                val addressCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressCol) ?: continue
                    if (!UpiParser.isBankSender(sender)) continue
                    val body = cursor.getString(bodyCol) ?: continue
                    val parsed = UpiParser.parse(body) ?: continue
                    if (repository.addCaptured(parsed, cursor.getLong(dateCol), TxnSource.SMS, body)) added++
                }
            }
            added
        }
}
