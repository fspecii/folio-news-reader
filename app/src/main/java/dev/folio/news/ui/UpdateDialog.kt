package dev.folio.news.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.folio.news.AppViewModel
import dev.folio.news.AppViewModel.UpdateState

/** Shown when a newer GitHub release exists. Installing hands the APK to the system installer. */
@Composable
fun UpdateDialog(vm: AppViewModel) {
    val u = vm.update
    val info = when (u) { is UpdateState.Available -> u.info; is UpdateState.Downloading -> u.info; else -> return }
    if (vm.updateDismissed && u is UpdateState.Available) return
    AlertDialog(
        onDismissRequest = { vm.updateDismissed = true },
        title = { Text("Folio ${info.version} is available") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("You have ${vm.versionName}.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                if (info.notes.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(info.notes.take(900), fontSize = 14.sp) }
                if (u is UpdateState.Downloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { u.progress }, Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.installUpdate(info) }, enabled = u !is UpdateState.Downloading) { Text(if (u is UpdateState.Downloading) "Downloading…" else "Update") } },
        dismissButton = { TextButton(onClick = { vm.updateDismissed = true }) { Text("Later") } },
    )
}
