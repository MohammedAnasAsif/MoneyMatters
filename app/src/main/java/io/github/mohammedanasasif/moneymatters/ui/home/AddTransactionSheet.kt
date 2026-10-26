package io.github.mohammedanasasif.moneymatters.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mohammedanasasif.moneymatters.data.TxnType
import io.github.mohammedanasasif.moneymatters.parse.Categories
import io.github.mohammedanasasif.moneymatters.ui.ChipRow
import io.github.mohammedanasasif.moneymatters.ui.formatDateTime
import io.github.mohammedanasasif.moneymatters.ui.parseAmountPaise
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onSave: (amountPaise: Long, type: TxnType, payee: String?, category: String?, timestamp: Long) -> Unit,
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxnType.DEBIT) }
    var category by remember { mutableStateOf<String?>(null) }
    var dateTime by remember { mutableStateOf(LocalDateTime.now().withSecond(0).withNano(0)) }
    val amountPaise = parseAmountPaise(amountText)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Add cash transaction", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            ChipRow(
                options = listOf(TxnType.DEBIT, TxnType.CREDIT),
                selected = type,
                label = { if (it == TxnType.DEBIT) "Spent" else "Received" },
                onSelect = { type = it },
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Amount (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = payee,
                onValueChange = { payee = it },
                label = { Text(if (type == TxnType.DEBIT) "Paid to / what for" else "Received from") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Category", color = AppColors.Muted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Categories.all) { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = if (category == c) null else c },
                        label = { Text(c) },
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> dateTime = LocalDateTime.of(year, month + 1, day, hour, minute) },
                                dateTime.hour,
                                dateTime.minute,
                                false,
                            ).show()
                        },
                        dateTime.year,
                        dateTime.monthValue - 1,
                        dateTime.dayOfMonth,
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(formatDateTime(dateTime))
            }

            Button(
                onClick = {
                    if (amountPaise != null) {
                        val millis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onSave(amountPaise, type, payee.trim().ifBlank { null }, category, millis)
                        onDismiss()
                    }
                },
                enabled = amountPaise != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
