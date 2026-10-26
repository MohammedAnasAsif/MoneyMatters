package io.github.mohammedanasasif.moneymatters.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mohammedanasasif.moneymatters.data.Transaction
import io.github.mohammedanasasif.moneymatters.ui.ChipRow
import io.github.mohammedanasasif.moneymatters.ui.changeColor
import io.github.mohammedanasasif.moneymatters.ui.formatChange
import io.github.mohammedanasasif.moneymatters.ui.formatDateTime
import io.github.mohammedanasasif.moneymatters.ui.formatInr
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors

/** The Stocks "single stock" view: big number, change, scrubbable chart and range chips. */
@Composable
fun TickerDetailScreen(vm: HomeViewModel, onBack: () -> Unit) {
    val state by vm.detail.collectAsStateWithLifecycle()
    var scrub by remember { mutableStateOf<ChartPoint?>(null) }
    var selected by remember { mutableStateOf<Transaction?>(null) }
    val d = state ?: return

    val change = d.previous?.let { d.value - it }
    val color = if (change == null) AppColors.Up else changeColor(change, d.upIsGood)
    val lineColor = if (change == null || change == 0L) (if (d.upIsGood) AppColors.Up else AppColors.Down) else color

    LazyColumn(
        Modifier.fillMaxSize().background(AppColors.Background).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(d.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(d.subtitle, color = AppColors.Muted, fontSize = 15.sp)
                }
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(AppColors.Surface),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = AppColors.Muted)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    formatInr(scrub?.value ?: d.value),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                val activeScrub = scrub
                when {
                    activeScrub != null -> Text(formatDateTime(activeScrub.time), color = AppColors.Muted, fontSize = 15.sp)
                    change != null -> {
                        val previous = d.previous ?: 0L
                        Text(
                            "${formatChange(change, previous, false)} (${formatChange(change, previous, true)}) vs the period before",
                            color = if (change == 0L) AppColors.Muted else color,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    else -> Text(d.range.title, color = AppColors.Muted, fontSize = 15.sp)
                }
            }
        }
        item {
            StockChart(
                points = d.points,
                start = d.rangeStart,
                end = d.rangeEnd,
                baseline = d.previous,
                lineColor = lineColor,
                onScrub = { scrub = it },
            )
        }
        item {
            ChipRow(
                options = Range.entries,
                selected = d.range,
                label = { it.label },
                onSelect = { vm.setDetailRange(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                spread = true,
            )
            HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
        }
        item {
            Text(
                "Transactions",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
            )
        }
        if (d.transactions.isEmpty()) {
            item { Text("Nothing in this period.", color = AppColors.Muted, modifier = Modifier.padding(20.dp)) }
        }
        items(d.transactions, key = { it.id }) { t -> TransactionRow(t) { selected = t } }
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
