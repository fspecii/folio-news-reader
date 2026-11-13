package dev.folio.news.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.folio.news.AppViewModel
import dev.folio.news.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(vm: AppViewModel) {
    ModalBottomSheet(
        onDismissRequest = { vm.showSettings = false },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).navigationBarsPadding()) {
            Text("Appearance", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = vm.themeMode == m, onClick = { vm.updateTheme(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) { Text(m.name) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Reading text size", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Slider(value = vm.fontScale, onValueChange = { vm.updateFontScale(it) }, valueRange = 0.8f..1.6f)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Two-page spread", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Show two pages side by side when unfolded", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Switch(checked = vm.twoPage, onCheckedChange = { vm.updateTwoPage(it) })
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Smart feed", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Ranks stories by what you open and skip. Off = newest first.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Switch(checked = vm.smartFeed, onCheckedChange = { vm.updateSmartFeed(it) })
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = { vm.showSettings = false; vm.showSources = true }, Modifier.fillMaxWidth()) { Text("Browse & manage sources") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.showSettings = false; vm.showWizard = true }, Modifier.fillMaxWidth()) { Text("Change my interests") }
            if (vm.muted.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Muted publishers", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                vm.muted.sorted().forEach { n ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(n, Modifier.weight(1f))
                        TextButton(onClick = { vm.unmute(n) }) { Text("Unmute") }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Updates", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-update from GitHub", fontSize = 16.sp)
                    Text("Checks for a new release when you open Folio", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Switch(checked = vm.autoUpdate, onCheckedChange = { vm.updateAuto(it) })
            }
            Spacer(Modifier.height(8.dp))
            val u = vm.update
            OutlinedButton(
                onClick = { if (u is AppViewModel.UpdateState.Available) vm.installUpdate(u.info) else vm.checkForUpdate(manual = true) },
                Modifier.fillMaxWidth(),
                enabled = u !is AppViewModel.UpdateState.Checking && u !is AppViewModel.UpdateState.Downloading,
            ) {
                Text(when (u) {
                    is AppViewModel.UpdateState.Checking -> "Checking\u2026"
                    is AppViewModel.UpdateState.Available -> "Install Folio ${u.info.version}"
                    is AppViewModel.UpdateState.Downloading -> "Downloading ${(u.progress * 100).toInt()}%"
                    else -> "Check for updates"
                })
            }
            Text(
                when (u) {
                    is AppViewModel.UpdateState.UpToDate -> "You're on the latest version (${vm.versionName})."
                    is AppViewModel.UpdateState.Failed -> u.message
                    else -> "Version ${vm.versionName}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
            )
            TextButton(onClick = { vm.resetLearning() }, Modifier.fillMaxWidth()) { Text("Reset what Folio learned", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
