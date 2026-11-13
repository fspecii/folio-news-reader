package dev.folio.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import dev.folio.news.AppViewModel
import dev.folio.news.data.Article
import dev.folio.news.data.Magazine

/** Your collections of saved stories (the Flipboard "magazines" idea, kept on the device). */
@Composable
fun MagazinesScreen(vm: AppViewModel) {
    var openId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val mag = vm.magazines.firstOrNull { it.id == openId }
    BackHandler { if (mag != null) openId = null else vm.showMagazines = false }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (mag != null) openId = null else vm.showMagazines = false }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(mag?.name ?: "Magazines", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.weight(1f))
            if (mag == null) IconButton(onClick = { creating = true }) { Icon(Icons.Default.Add, "New magazine") }
            else if (mag.id != "later") TextButton(onClick = { vm.deleteMagazine(mag.id); openId = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
        if (mag == null) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vm.magazines, key = { it.id }) { m -> MagazineCard(m) { openId = m.id } }
                item {
                    Text(
                        "Save a story with the star in the reader, or use the menu to pick a magazine.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    )
                }
            }
        } else if (mag.articles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("Nothing here yet.\nOpen a story and tap the star.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(mag.articles, key = { it.id }) { a -> SavedRow(a, onOpen = { vm.open(a) }, onRemove = { vm.toggleSave(a, mag) }) }
            }
        }
    }
    if (creating) NameDialog("New magazine", onDone = { vm.newMagazine(it); creating = false }, onCancel = { creating = false })
}

@Composable
private fun MagazineCard(m: Magazine, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val cover = m.articles.firstNotNullOfOrNull { it.image }
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))) {
            if (cover != null) AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(m.name, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text("${m.articles.size} ${if (m.articles.size == 1) "story" else "stories"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SavedRow(a: Article, onOpen: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
        if (a.image != null) {
            AsyncImage(a.image, null, Modifier.size(78.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(a.sourceName.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp, maxLines = 1)
            Text(a.title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** "Flip into magazine": choose where to keep a story, or start a new magazine. */
@Composable
fun SaveToMagazineDialog(vm: AppViewModel, a: Article) {
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { vm.saveTarget = null },
        title = { Text("Save to magazine") },
        text = {
            Column {
                vm.magazines.forEach { m ->
                    val on = vm.savedIn(a, m)
                    Row(Modifier.fillMaxWidth().clickable { vm.toggleSave(a, m) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = on, onCheckedChange = { vm.toggleSave(a, m) })
                        Text(m.name, modifier = Modifier.weight(1f))
                        Text("${m.articles.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { creating = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("New magazine") }
            }
        },
        confirmButton = { TextButton(onClick = { vm.saveTarget = null }) { Text("Done") } },
    )
    if (creating) NameDialog("New magazine", onDone = { vm.newMagazine(it, a); creating = false }, onCancel = { creating = false })
}

@Composable
private fun NameDialog(title: String, onDone: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, placeholder = { Text("e.g. Long reads") }) },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onDone(text) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
