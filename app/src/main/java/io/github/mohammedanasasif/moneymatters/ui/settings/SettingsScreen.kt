package io.github.mohammedanasasif.moneymatters.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.mohammedanasasif.moneymatters.MoneyMattersApp
import io.github.mohammedanasasif.moneymatters.capture.SmsImporter
import io.github.mohammedanasasif.moneymatters.ui.BackBar
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyMattersApp
    val scope = rememberCoroutineScope()

    // Permissions are granted in system screens, so re-check whenever we come back.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val smsGranted = remember(refresh) { hasSmsPermission(context) }
    val notificationsGranted = remember(refresh) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    var importing by remember { mutableStateOf(false) }
    var logCredits by remember { mutableStateOf(app.prefs.logCredits) }

    fun runImport() {
        importing = true
        scope.launch {
            val added = SmsImporter.import(context, app.repository)
            importing = false
            Toast.makeText(context, "Imported $added payments", Toast.LENGTH_SHORT).show()
        }
    }

    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        refresh++
        if (result.values.all { it }) runImport()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openOutputStream(uri)?.let { app.repository.exportCsv(it) }
                Toast.makeText(context, "Exported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(containerColor = AppColors.Background, topBar = { BackBar("Settings", onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Auto-capture")
            PermissionCard(
                title = "Bank SMS",
                body = "Reads the debit SMS your bank sends. Works for payments from every UPI app.",
                granted = smsGranted,
                actionLabel = "Allow",
                onAction = { smsLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)) },
            )
            PermissionCard(
                title = "UPI app notifications",
                body = "Backup for late SMS: reads \"payment successful\" alerts from GPay, PhonePe, Paytm and other UPI apps only.",
                granted = notificationsGranted,
                actionLabel = "Allow",
                onAction = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
            Text(
                "If Android says a setting is restricted, open App info, tap ⋮ and choose " +
                    "\"Allow restricted settings\", then try again. Android does this for apps installed outside the Play Store.",
                color = AppColors.Muted,
                fontSize = 13.sp,
            )
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                )
            }) { Text("Open App info", color = AppColors.Accent) }
            OutlinedButton(
                onClick = { runImport() },
                enabled = smsGranted && !importing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (importing) "Importing…" else "Import UPI SMS from the last 90 days")
            }

            SectionTitle("Options")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Log money received", color = Color.White)
                    Text("Also record UPI credits, not just payments", color = AppColors.Muted, fontSize = 13.sp)
                }
                Switch(
                    checked = logCredits,
                    onCheckedChange = {
                        logCredits = it
                        app.prefs.logCredits = it
                    },
                )
            }

            SectionTitle("Data")
            OutlinedButton(
                onClick = { exportLauncher.launch("moneymatters-transactions.csv") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export CSV") }

            Text(
                "MoneyMatters is open source (GPL-3.0). It has no internet permission, " +
                    "so your transactions never leave this phone.",
                color = AppColors.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun hasSmsPermission(context: Context): Boolean =
    listOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = AppColors.Muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                if (granted) "On" else "Off",
                color = if (granted) AppColors.Up else AppColors.Muted,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(body, color = AppColors.Muted, fontSize = 14.sp)
        if (!granted) Button(onClick = onAction) { Text(actionLabel) }
    }
}
