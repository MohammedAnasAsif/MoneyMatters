package io.github.mohammedanasasif.moneymatters.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mohammedanasasif.moneymatters.data.TxnType
import io.github.mohammedanasasif.moneymatters.data.displayName
import io.github.mohammedanasasif.moneymatters.ui.AmountPill
import io.github.mohammedanasasif.moneymatters.ui.BackBar
import io.github.mohammedanasasif.moneymatters.ui.formatDateTime
import io.github.mohammedanasasif.moneymatters.ui.formatInr
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors

/** Payments the parser wasn't sure about. Nothing here counts towards totals until confirmed. */
@Composable
fun ReviewScreen(vm: HomeViewModel, onBack: () -> Unit) {
    val pending by vm.needsReview.collectAsStateWithLifecycle()

    Scaffold(containerColor = AppColors.Background, topBar = { BackBar("Review", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pending.isEmpty()) {
                item { Text("All caught up.", color = AppColors.Muted) }
            }
            items(pending, key = { it.id }) { t ->
                val isCredit = t.type == TxnType.CREDIT
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.displayName(), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(formatDateTime(t.timestamp), color = AppColors.Muted, fontSize = 13.sp)
                        }
                        AmountPill(
                            (if (isCredit) "+" else "−") + formatInr(t.amountPaise),
                            if (isCredit) AppColors.Up else AppColors.Down,
                        )
                    }
                    t.rawText?.let { Text(it, color = AppColors.Muted, fontSize = 13.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { vm.confirm(t) }) { Text("Confirm") }
                        OutlinedButton(onClick = { vm.delete(t) }) { Text("Discard") }
                    }
                }
            }
        }
    }
}
