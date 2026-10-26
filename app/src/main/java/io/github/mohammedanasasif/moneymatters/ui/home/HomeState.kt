package io.github.mohammedanasasif.moneymatters.ui.home

import io.github.mohammedanasasif.moneymatters.data.Transaction
import io.github.mohammedanasasif.moneymatters.data.TxnStatus
import io.github.mohammedanasasif.moneymatters.data.TxnType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Range(val label: String, val title: String) {
    D1("1D", "Today"),
    W1("1W", "Past week"),
    M1("1M", "Past month"),
    M3("3M", "Past 3 months"),
    Y1("1Y", "Past year"),
    ALL("ALL", "All time"),
}

enum class TickerKind { SPENT, RECEIVED, CATEGORY }

const val UNCATEGORISED = "Uncategorised"

/**
 * One row of the watchlist, like a stock in Apple Stocks: a name, this month's total
 * as the "price", and the change against the same days of last month.
 */
data class Ticker(
    val id: String,
    val kind: TickerKind,
    val title: String,
    val subtitle: String,
    val value: Long,
    val previous: Long,
    /** Cumulative total across the month so far, for the sparkline. */
    val spark: List<Long>,
) {
    val change: Long get() = value - previous

    /** More money received is good; more money spent is not. */
    val upIsGood: Boolean get() = kind == TickerKind.RECEIVED
}

data class HomeState(
    val dateLabel: String,
    val monthLabel: String,
    val tickers: List<Ticker>,
    val recent: List<Transaction>,
    val reviewCount: Int,
    val hasAnyData: Boolean,
)

data class ChartPoint(val time: Long, val value: Long)

data class DetailState(
    val id: String,
    val title: String,
    val subtitle: String,
    val upIsGood: Boolean,
    val range: Range,
    val value: Long,
    /** Total in the equally long period just before this one; null for ALL. */
    val previous: Long?,
    val rangeStart: Long,
    val rangeEnd: Long,
    val points: List<ChartPoint>,
    val transactions: List<Transaction>,
)

private const val DAY = 86_400_000L
private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)
private val monthName = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

fun tickerMatches(id: String, t: Transaction): Boolean = when {
    id == "spent" -> t.type == TxnType.DEBIT
    id == "received" -> t.type == TxnType.CREDIT
    id.startsWith("cat:") -> t.type == TxnType.DEBIT && (t.category ?: UNCATEGORISED) == id.removePrefix("cat:")
    else -> false
}

private fun tickerTitle(id: String): String = when {
    id == "spent" -> "Spent"
    id == "received" -> "Received"
    else -> id.removePrefix("cat:")
}

fun buildHomeState(all: List<Transaction>, now: Long): HomeState {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val previousStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
    // Compare like with like: the first N days of this month against the first N days of last month.
    val previousEnd = minOf(previousStart + (now - monthStart), monthStart)
    val confirmed = all.filter { it.status == TxnStatus.CONFIRMED }

    fun ticker(id: String, kind: TickerKind, subtitle: (Int) -> String): Ticker {
        val matching = confirmed.filter { tickerMatches(id, it) }
        val current = matching.filter { it.timestamp in monthStart..now }
        val previous = matching.filter { it.timestamp >= previousStart && it.timestamp < previousEnd }
        return Ticker(
            id = id,
            kind = kind,
            title = tickerTitle(id),
            subtitle = subtitle(current.size),
            value = current.sumOf { it.amountPaise },
            previous = previous.sumOf { it.amountPaise },
            spark = cumulative(current, monthStart, now),
        )
    }

    fun payments(n: Int) = if (n == 1) "1 payment this month" else "$n payments this month"

    val tickers = buildList {
        add(ticker("spent", TickerKind.SPENT) { "All UPI and cash spending" })
        if (confirmed.any { it.type == TxnType.CREDIT }) {
            add(ticker("received", TickerKind.RECEIVED) { "Money in" })
        }
        confirmed
            .filter { it.type == TxnType.DEBIT && it.timestamp >= previousStart }
            .map { it.category ?: UNCATEGORISED }
            .distinct()
            .map { ticker("cat:$it", TickerKind.CATEGORY, ::payments) }
            .sortedByDescending { it.value }
            .forEach { add(it) }
    }

    return HomeState(
        dateLabel = dayMonth.format(today),
        monthLabel = monthName.format(today),
        tickers = tickers,
        recent = confirmed.sortedByDescending { it.timestamp }.take(200),
        reviewCount = all.count { it.status == TxnStatus.NEEDS_REVIEW },
        hasAnyData = all.isNotEmpty(),
    )
}

fun buildDetailState(all: List<Transaction>, id: String, range: Range, now: Long): DetailState {
    val matching = all.filter { it.status == TxnStatus.CONFIRMED && tickerMatches(id, it) }

    val start = when (range) {
        Range.D1 -> LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        Range.W1 -> now - 7 * DAY
        Range.M1 -> now - 30 * DAY
        Range.M3 -> now - 90 * DAY
        Range.Y1 -> now - 365 * DAY
        Range.ALL -> (matching.minOfOrNull { it.timestamp } ?: (now - 30 * DAY)).coerceAtMost(now - DAY)
    }

    val inRange = matching.filter { it.timestamp in start..now }.sortedBy { it.timestamp }
    val previous = if (range == Range.ALL) {
        null
    } else {
        val length = now - start
        matching.filter { it.timestamp >= start - length && it.timestamp < start }.sumOf { it.amountPaise }
    }

    val points = buildList {
        add(ChartPoint(start, 0))
        var running = 0L
        for (t in inRange) {
            running += t.amountPaise
            add(ChartPoint(t.timestamp, running))
        }
        add(ChartPoint(now, running))
    }

    return DetailState(
        id = id,
        title = tickerTitle(id),
        subtitle = when {
            id == "spent" -> "All spending"
            id == "received" -> "Money in"
            else -> "Category"
        },
        upIsGood = id == "received",
        range = range,
        value = inRange.sumOf { it.amountPaise },
        previous = previous,
        rangeStart = start,
        rangeEnd = now,
        points = points,
        transactions = inRange.reversed(),
    )
}

private fun cumulative(txns: List<Transaction>, start: Long, end: Long, buckets: Int = 24): List<Long> {
    val span = (end - start).coerceAtLeast(1L)
    val sums = LongArray(buckets)
    for (t in txns) {
        val i = ((t.timestamp - start).toDouble() / span * buckets).toInt().coerceIn(0, buckets - 1)
        sums[i] += t.amountPaise
    }
    var running = 0L
    return listOf(0L) + sums.map { running += it; running }
}
