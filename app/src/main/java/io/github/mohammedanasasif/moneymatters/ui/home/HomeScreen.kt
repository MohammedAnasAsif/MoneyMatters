package io.github.mohammedanasasif.moneymatters.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mohammedanasasif.moneymatters.data.Transaction
import io.github.mohammedanasasif.moneymatters.ui.AmountPill
import io.github.mohammedanasasif.moneymatters.ui.ChipRow
import io.github.mohammedanasasif.moneymatters.ui.Sparkline
import io.github.mohammedanasasif.moneymatters.ui.formatDateTime
import io.github.mohammedanasasif.moneymatters.ui.formatInr
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(vm: HomeViewModel, onOpenSettings: () -> Unit, onOpenReview: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var scrub by remember { mutableStateOf<ChartPoint?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Transaction?>(null) }
    val s = state

    BackHandler(enabled = s?.filter != null) { vm.setFilter(null) }

    Scaffold(
        containerColor = AppColors.Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add cash transaction")
            }
        },
    ) { padding ->
        if (s == null) return@Scaffold
        val spendingUp = s.previousSpent != null && s.totalSpent > s.previousSpent
        val lineColor = if (spendingUp) AppColors.Down else AppColors.Up

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            item { TopBar(onOpenSettings) }
            item { Header(s, scrub, lineColor) }
            item {
                StockChart(
                    points = s.points,
                    start = s.rangeStart,
                    end = s.rangeEnd,
                    baseline = s.previousSpent,
                    lineColor = lineColor,
                    onScrub = { scrub = it },
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            item {
                ChipRow(
                    options = Range.entries,
                    selected = s.range,
                    label = { it.label },
                    onSelect = { vm.setRange(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    spread = true,
                )
            }
            if (s.reviewCount > 0) {
                item { ReviewBanner(s.reviewCount, onOpenReview) }
            }
            item {
                val filter = s.filter
                if (filter != null) {
                    FilterBar(filter) { vm.setFilter(null) }
                } else {
                    ChipRow(
                        options = ListMode.entries,
                        selected = s.mode,
                        label = { it.label },
                        onSelect = { vm.setMode(it) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                    )
                }
                HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
            }
            if (!s.hasAnyData) {
                item { EmptyState(onOpenSettings) }
            } else if (s.rows.isEmpty()) {
                item {
                    Text(
                        "Nothing in this period.",
                        color = AppColors.Muted,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(s.rows, key = { it.key }) { row ->
                ListRowItem(row) {
                    // A single payment opens its details; a payee/category row narrows the screen to it.
                    val txn = row.transaction
                    if (txn != null) selected = txn
                    if (txn == null) row.filter?.let { vm.setFilter(it) }
                }
            }
        }
    }

    if (showAdd) {
        AddTransactionSheet(
            onDismiss = { showAdd = false },
            onSave = { amount, type, payee, category, time -> vm.addManual(amount, type, payee, category, time) },
        )
    }
    selected?.let { txn ->
        TransactionDetailSheet(
            transaction = txn,
            onDismiss = { selected = null },
            onUpdate = { vm.update(it) },
            onDelete = { vm.delete(it) },
        )
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "MoneyMatters",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
        }
    }
}

@Composable
private fun Header(s: HomeState, scrub: ChartPoint?, lineColor: Color) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            if (scrub != null) formatDateTime(scrub.time) else s.title,
            color = AppColors.Muted,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatInr(scrub?.value ?: s.totalSpent),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            val previous = s.previousSpent
            if (scrub == null && previous != null && previous > 0) {
                Spacer(Modifier.width(12.dp))
                val pct = (s.totalSpent - previous) * 100.0 / previous
                val arrow = if (pct >= 0) "▲" else "▼"
                AmountPill("$arrow ${String.format(Locale.US, "%.1f", abs(pct))}%", lineColor)
            }
        }
        val previous = s.previousSpent
        if (scrub == null && previous != null) {
            Text(
                "vs ${formatInr(previous)} in the period before",
                color = AppColors.Muted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun ReviewBanner(count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (count == 1) "1 payment needs a quick check" else "$count payments need a quick check",
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text("Review ›", color = AppColors.Accent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FilterBar(filter: Filter, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            filter.key,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear) { Text("Show all", color = AppColors.Accent) }
    }
}

@Composable
private fun ListRowItem(row: ListRow, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    row.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(row.subtitle, color = AppColors.Muted, fontSize = 13.sp, maxLines = 1)
            }
            if (row.spark.isNotEmpty()) {
                Sparkline(row.spark, AppColors.Down, Modifier.size(width = 56.dp, height = 24.dp))
                Spacer(Modifier.width(12.dp))
            }
            AmountPill(
                (if (row.isCredit) "+" else "−") + formatInr(row.amountPaise),
                if (row.isCredit) AppColors.Up else AppColors.Down,
                Modifier.widthIn(min = 84.dp),
            )
        }
        HorizontalDivider(Modifier.padding(start = 16.dp), color = AppColors.Divider, thickness = 0.5.dp)
    }
}

@Composable
private fun EmptyState(onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .padding(20.dp),
    ) {
        Text("Start tracking", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Turn on auto-capture and every UPI payment, from any app, is logged here. " +
                "Paid in cash? Tap + to add it.",
            color = AppColors.Muted,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )
        Button(onClick = onOpenSettings) { Text("Turn on auto-capture") }
    }
}
