package io.github.mohammedanasasif.moneymatters.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mohammedanasasif.moneymatters.data.Transaction
import io.github.mohammedanasasif.moneymatters.data.TxnSource
import io.github.mohammedanasasif.moneymatters.data.TxnType
import io.github.mohammedanasasif.moneymatters.data.displayName
import io.github.mohammedanasasif.moneymatters.ui.formatInr
import io.github.mohammedanasasif.moneymatters.ui.formatShort
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors

/** One payment: money in shows green with "+", money out red with "−". */
@Composable
fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    val isCredit = transaction.type == TxnType.CREDIT
    val via = if (transaction.source == TxnSource.MANUAL_CASH) "Cash" else "UPI"
    val details = listOfNotNull(formatShort(transaction.timestamp), via, transaction.category).joinToString(" · ")

    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.displayName(),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(details, color = AppColors.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                (if (isCredit) "+" else "−") + formatInr(transaction.amountPaise),
                color = if (isCredit) AppColors.Up else AppColors.Down,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        HorizontalDivider(Modifier.padding(start = 20.dp), color = AppColors.Divider, thickness = 0.5.dp)
    }
}
