package io.github.mohammedanasasif.moneymatters.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
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
import io.github.mohammedanasasif.moneymatters.ui.Sparkline
import io.github.mohammedanasasif.moneymatters.ui.changeColor
import io.github.mohammedanasasif.moneymatters.ui.formatChange
import io.github.mohammedanasasif.moneymatters.ui.formatInr
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors

/**
 * Laid out like the Apple Stocks home screen: a watchlist of "tickers" (Spent, Received and
 * one per category), with a pull-up sheet of transactions where Stocks has its news.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenSettings: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenTicker: (String) -> Unit,
) {
    val state by vm.home.collectAsStateWithLifecycle()
    var showPercent by rememberSaveable { mutableStateOf(false) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Transaction?>(null) }
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val peekHeight = 150.dp + navBarHeight

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(),
        sheetPeekHeight = peekHeight,
        sheetContainerColor = AppColors.Surface,
        sheetTonalElevation = 0.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetDragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.Muted) },
        containerColor = AppColors.Background,
        sheetContent = {
            val s = state
            if (s != null) {
                TransactionsSheet(s, onOpenReview, onSelect = { selected = it })
            }
        },
    ) {
        val s = state ?: return@BottomSheetScaffold
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = peekHeight + 16.dp),
        ) {
            item { Header(s.dateLabel, onAdd = { showAdd = true }, onOpenSettings = onOpenSettings) }
            if (!s.hasAnyData) {
                item { EmptyState(onOpenSettings) }
            }
            items(s.tickers, key = { it.id }) { ticker ->
                TickerRow(
                    ticker = ticker,
                    showPercent = showPercent,
                    onClick = { onOpenTicker(ticker.id) },
                    onTogglePill = { showPercent = !showPercent },
                )
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
private fun Header(dateLabel: String, onAdd: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text("MoneyMatters", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(dateLabel, color = AppColors.Muted, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(AppColors.Surface),
        ) {
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add cash transaction", tint = Color.White)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }
    }
}

@Composable
private fun TickerRow(ticker: Ticker, showPercent: Boolean, onClick: () -> Unit, onTogglePill: () -> Unit) {
    val color = changeColor(ticker.change, ticker.upIsGood)
    val lineColor = if (ticker.change == 0L) AppColors.Muted else color

    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    ticker.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    ticker.subtitle,
                    color = AppColors.Muted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Sparkline(
                values = ticker.spark,
                baseline = ticker.previous,
                color = lineColor,
                modifier = Modifier.size(width = 84.dp, height = 40.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(min = 96.dp)) {
                Text(formatInr(ticker.value), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                AmountPill(
                    formatChange(ticker.change, ticker.previous, showPercent),
                    color,
                    Modifier.widthIn(min = 92.dp).clickable(onClick = onTogglePill),
                )
            }
        }
        HorizontalDivider(Modifier.padding(start = 20.dp), color = AppColors.Divider, thickness = 0.5.dp)
    }
}

@Composable
private fun TransactionsSheet(s: HomeState, onOpenReview: () -> Unit, onSelect: (Transaction) -> Unit) {
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
        Text(
            "Transactions",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "Latest UPI and cash payments",
            color = AppColors.Muted,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
        LazyColumn(Modifier.weight(1f)) {
            if (s.reviewCount > 0) {
                item { ReviewBanner(s.reviewCount, onOpenReview) }
            }
            if (s.recent.isEmpty()) {
                item { Text("No transactions yet.", color = AppColors.Muted, modifier = Modifier.padding(20.dp)) }
            }
            items(s.recent, key = { it.id }) { t -> TransactionRow(t) { onSelect(t) } }
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
            .background(AppColors.SurfaceHigh)
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
private fun EmptyState(onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Start tracking", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Turn on auto-capture and every UPI payment, from any app, is logged here. " +
                "Paid in cash? Tap + to add it.",
            color = AppColors.Muted,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenSettings) { Text("Turn on auto-capture") }
    }
}
