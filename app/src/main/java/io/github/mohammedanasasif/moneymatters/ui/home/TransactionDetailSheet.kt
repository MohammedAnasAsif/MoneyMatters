package io.github.mohammedanasasif.moneymatters.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mohammedanasasif.moneymatters.data.Transaction
import io.github.mohammedanasasif.moneymatters.data.TxnSource
import io.github.mohammedanasasif.moneymatters.data.TxnStatus
import io.github.mohammedanasasif.moneymatters.data.TxnType
import io.github.mohammedanasasif.moneymatters.data.displayName
import io.github.mohammedanasasif.moneymatters.parse.Categories
import io.github.mohammedanasasif.moneymatters.ui.formatDateTime
import io.github.mohammedanasasif.moneymatters.ui.formatInr
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onUpdate: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    var current by remember(transaction.id) { mutableStateOf(transaction) }
    val isCredit = current.type == TxnType.CREDIT

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(current.displayName(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                (if (isCredit) "+" else "−") + formatInr(current.amountPaise),
                color = if (isCredit) AppColors.Up else AppColors.Down,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )

            DetailLine("When", formatDateTime(current.timestamp))
            current.vpa?.let { DetailLine("UPI ID", it) }
            current.upiRef?.let { DetailLine("UPI ref", it) }
            DetailLine(
                "Source",
                when (current.source) {
                    TxnSource.SMS -> "Bank SMS"
                    TxnSource.NOTIFICATION -> "UPI app notification"
                    TxnSource.MANUAL_CASH -> "Cash, added by you"
                },
            )

            Text("Category", color = AppColors.Muted, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Categories.all) { c ->
                    FilterChip(
                        selected = current.category == c,
                        onClick = {
                            current = current.copy(category = if (current.category == c) null else c)
                            onUpdate(current)
                        },
                        label = { Text(c) },
                    )
                }
            }

            current.rawText?.let { raw ->
                Text("Original message", color = AppColors.Muted, fontSize = 13.sp)
                Text(raw, color = AppColors.Muted, fontSize = 13.sp)
            }

            if (current.status == TxnStatus.NEEDS_REVIEW) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        onUpdate(current.copy(status = TxnStatus.CONFIRMED))
                        onDismiss()
                    }) { Text("Confirm") }
                    OutlinedButton(onClick = {
                        onDelete(current)
                        onDismiss()
                    }) { Text("Discard") }
                }
            } else {
                TextButton(onClick = {
                    onDelete(current)
                    onDismiss()
                }) { Text("Delete transaction", color = AppColors.Down) }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(label, color = AppColors.Muted, modifier = Modifier.weight(1f))
            Text(value, color = Color.White)
        }
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
    }
}
