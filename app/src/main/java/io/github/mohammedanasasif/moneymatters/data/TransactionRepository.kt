package io.github.mohammedanasasif.moneymatters.data

import io.github.mohammedanasasif.moneymatters.parse.Categories
import io.github.mohammedanasasif.moneymatters.parse.ParsedTxn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

class TransactionRepository(
    private val dao: TransactionDao,
    private val prefs: Prefs,
) {
    val all: Flow<List<Transaction>> = dao.observeAll()

    // SMS and notification for the same payment can arrive at the same moment.
    private val captureLock = Mutex()

    suspend fun addManual(
        amountPaise: Long,
        type: TxnType,
        payee: String?,
        category: String?,
        timestamp: Long,
    ) {
        dao.insert(
            Transaction(
                amountPaise = amountPaise,
                type = type,
                timestamp = timestamp,
                payee = payee,
                category = category ?: Categories.guess(payee),
                source = TxnSource.MANUAL_CASH,
            )
        )
    }

    suspend fun update(transaction: Transaction) = dao.update(transaction)

    suspend fun delete(transaction: Transaction) = dao.delete(transaction)

    /**
     * Saves an auto-captured payment unless it is a duplicate of one already stored.
     * Returns true when a new row was inserted.
     */
    suspend fun addCaptured(
        parsed: ParsedTxn,
        timestamp: Long,
        source: TxnSource,
        rawText: String,
    ): Boolean = captureLock.withLock {
        if (parsed.type == TxnType.CREDIT && !prefs.logCredits) return@withLock false

        parsed.upiRef?.let { if (dao.countByRef(it) > 0) return@withLock false }

        val window = 3 * 60_000L
        val nearby = dao.findNear(parsed.amountPaise, parsed.type.name, timestamp - window, timestamp + window)
        val duplicate = nearby.firstOrNull { other ->
            other.rawText == rawText ||
                (other.source != source && (other.upiRef == null || parsed.upiRef == null))
        }
        if (duplicate != null) {
            // Same payment seen twice: keep one row, filling in anything the first source missed.
            val merged = duplicate.copy(
                upiRef = duplicate.upiRef ?: parsed.upiRef,
                payee = duplicate.payee ?: parsed.payee,
                vpa = duplicate.vpa ?: parsed.vpa,
                status = if (parsed.confident) TxnStatus.CONFIRMED else duplicate.status,
            )
            if (merged != duplicate) dao.update(merged)
            return@withLock false
        }

        dao.insert(
            Transaction(
                amountPaise = parsed.amountPaise,
                type = parsed.type,
                timestamp = timestamp,
                payee = parsed.payee,
                vpa = parsed.vpa,
                upiRef = parsed.upiRef,
                category = Categories.guess(parsed.payee, parsed.vpa),
                source = source,
                status = if (parsed.confident) TxnStatus.CONFIRMED else TxnStatus.NEEDS_REVIEW,
                rawText = rawText,
            )
        )
        true
    }

    suspend fun exportCsv(out: OutputStream) = withContext(Dispatchers.IO) {
        val rows = dao.getAll()
        out.bufferedWriter().use { w ->
            w.appendLine("date,type,amount,payee,upi_id,upi_ref,category,source,status")
            for (t in rows) {
                val date = Instant.ofEpochMilli(t.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
                val fields = listOf(
                    date.toString(),
                    t.type.name,
                    BigDecimal(t.amountPaise).movePointLeft(2).toPlainString(),
                    t.payee,
                    t.vpa,
                    t.upiRef,
                    t.category,
                    t.source.name,
                    t.status.name,
                )
                w.appendLine(fields.joinToString(",") { csvField(it) })
            }
        }
    }

    private fun csvField(value: String?): String = when {
        value == null -> ""
        value.any { it == ',' || it == '"' || it == '\n' } -> "\"" + value.replace("\"", "\"\"") + "\""
        else -> value
    }
}
