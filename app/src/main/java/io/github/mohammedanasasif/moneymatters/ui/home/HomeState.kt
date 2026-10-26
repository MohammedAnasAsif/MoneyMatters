package io.github.mohammedanasasif.moneymatters.ui.home

import io.github.mohammedanasasif.moneymatters.data.Transaction
import io.github.mohammedanasasif.moneymatters.data.TxnSource
import io.github.mohammedanasasif.moneymatters.data.TxnStatus
import io.github.mohammedanasasif.moneymatters.data.TxnType
import io.github.mohammedanasasif.moneymatters.data.displayName
import io.github.mohammedanasasif.moneymatters.ui.formatShort
import java.time.LocalDate
import java.time.ZoneId

enum class Range(val label: String, val title: String) {
    D1("1D", "Today"),
    W1("1W", "Past week"),
    M1("1M", "Past month"),
    M3("3M", "Past 3 months"),
    Y1("1Y", "Past year"),
    ALL("ALL", "All time"),
}

enum class ListMode(val label: String) {
    RECENT("Recent"),
    PAYEES("Payees"),
    CATEGORIES("Categories"),
}

/** Narrows the whole screen (chart included) to one payee or category. */
data class Filter(val mode: ListMode, val key: String)

data class ChartPoint(val time: Long, val value: Long)

data class ListRow(
    val key: String,
    val title: String,
    val subtitle: String,
    val amountPaise: Long,
    val isCredit: Boolean,
    val spark: List<Long>,
    val transaction: Transaction?,
    val filter: Filter?,
)

data class HomeState(
    val range: Range,
    val mode: ListMode,
    val filter: Filter?,
    val title: String,
    val totalSpent: Long,
    /** Spend in the equally long period just before this one; null for ALL. */
    val previousSpent: Long?,
    val rangeStart: Long,
    val rangeEnd: Long,
    val points: List<ChartPoint>,
    val rows: List<ListRow>,
    val reviewCount: Int,
    val hasAnyData: Boolean,
)

private const val DAY = 86_400_000L

fun buildHomeState(
    all: List<Transaction>,
    range: Range,
    mode: ListMode,
    filter: Filter?,
    now: Long,
): HomeState {
    val confirmed = all.filter { it.status == TxnStatus.CONFIRMED }
    val scoped = if (filter == null) confirmed else confirmed.filter { groupKey(it, filter.mode) == filter.key }

    val start = when (range) {
        Range.D1 -> LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        Range.W1 -> now - 7 * DAY
        Range.M1 -> now - 30 * DAY
        Range.M3 -> now - 90 * DAY
        Range.Y1 -> now - 365 * DAY
        Range.ALL -> (scoped.minOfOrNull { it.timestamp } ?: (now - 30 * DAY)).coerceAtMost(now - DAY)
    }

    val inRange = scoped.filter { it.timestamp in start..now }
    val debits = inRange.filter { it.type == TxnType.DEBIT }.sortedBy { it.timestamp }
    val total = debits.sumOf { it.amountPaise }

    val previous = if (range == Range.ALL) {
        null
    } else {
        val length = now - start
        scoped.filter { it.type == TxnType.DEBIT && it.timestamp >= start - length && it.timestamp < start }
            .sumOf { it.amountPaise }
    }

    // Cumulative spend over the period: a line that only goes up, like a share price on a good day.
    val points = buildList {
        add(ChartPoint(start, 0))
        var running = 0L
        for (t in debits) {
            running += t.amountPaise
            add(ChartPoint(t.timestamp, running))
        }
        add(ChartPoint(now, running))
    }

    val rowMode = if (filter != null) ListMode.RECENT else mode
    val rows = when (rowMode) {
        ListMode.RECENT -> inRange.sortedByDescending { it.timestamp }.map { t ->
            ListRow(
                key = "t${t.id}",
                title = t.displayName(),
                subtitle = subtitle(t),
                amountPaise = t.amountPaise,
                isCredit = t.type == TxnType.CREDIT,
                spark = emptyList(),
                transaction = t,
                filter = null,
            )
        }
        ListMode.PAYEES, ListMode.CATEGORIES -> debits.groupBy { groupKey(it, rowMode) }
            .map { (key, txns) ->
                ListRow(
                    key = "g$key",
                    title = key,
                    subtitle = if (txns.size == 1) "1 payment" else "${txns.size} payments",
                    amountPaise = txns.sumOf { it.amountPaise },
                    isCredit = false,
                    spark = sparkline(txns, start, now),
                    transaction = null,
                    filter = Filter(rowMode, key),
                )
            }
            .sortedByDescending { it.amountPaise }
    }

    return HomeState(
        range = range,
        mode = mode,
        filter = filter,
        title = "${filter?.key ?: "Spent"} · ${range.title}",
        totalSpent = total,
        previousSpent = previous,
        rangeStart = start,
        rangeEnd = now,
        points = points,
        rows = rows,
        reviewCount = all.count { it.status == TxnStatus.NEEDS_REVIEW },
        hasAnyData = all.isNotEmpty(),
    )
}

private fun groupKey(t: Transaction, mode: ListMode): String = when (mode) {
    ListMode.PAYEES -> t.displayName()
    ListMode.CATEGORIES -> t.category ?: "Uncategorised"
    ListMode.RECENT -> ""
}

private fun subtitle(t: Transaction): String {
    val via = when {
        t.type == TxnType.CREDIT -> "Received"
        t.source == TxnSource.MANUAL_CASH -> "Cash"
        else -> "UPI"
    }
    return "${formatShort(t.timestamp)} · $via"
}

private fun sparkline(txns: List<Transaction>, start: Long, end: Long, buckets: Int = 16): List<Long> {
    val span = (end - start).coerceAtLeast(1L)
    val sums = LongArray(buckets)
    for (t in txns) {
        val i = ((t.timestamp - start).toDouble() / span * buckets).toInt().coerceIn(0, buckets - 1)
        sums[i] += t.amountPaise
    }
    var running = 0L
    return sums.map { running += it; running }
}
