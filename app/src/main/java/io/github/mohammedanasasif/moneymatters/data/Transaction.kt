package io.github.mohammedanasasif.moneymatters.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxnType { DEBIT, CREDIT }

enum class TxnSource { SMS, NOTIFICATION, MANUAL_CASH }

enum class TxnStatus { CONFIRMED, NEEDS_REVIEW }

@Entity(tableName = "transactions", indices = [Index("timestamp"), Index("upiRef")])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stored in paise so sums never hit floating-point rounding. */
    val amountPaise: Long,
    val type: TxnType,
    val timestamp: Long,
    val payee: String? = null,
    val vpa: String? = null,
    val upiRef: String? = null,
    val category: String? = null,
    val note: String? = null,
    val source: TxnSource,
    val status: TxnStatus = TxnStatus.CONFIRMED,
    /** The SMS or notification text this was parsed from, kept so users can check it. */
    val rawText: String? = null,
)

fun Transaction.displayName(): String =
    payee
        ?: vpa?.substringBefore('@')
        ?: note?.takeIf { it.isNotBlank() }
        ?: when {
            source == TxnSource.MANUAL_CASH -> "Cash"
            // Some banks (e.g. UCO) never name the other party in their SMS.
            type == TxnType.CREDIT -> "UPI money received"
            else -> "UPI payment"
        }
