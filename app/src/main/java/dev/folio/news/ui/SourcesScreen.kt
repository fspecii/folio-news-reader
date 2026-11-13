package dev.folio.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.folio.news.AppViewModel
import dev.folio.news.data.Catalog
import kotlinx.coroutines.launch

private val Serif = FontFamily.Serif

@Composable
fun SourcesScreen(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var showMine by remember { mutableStateOf(true) }
    val open = remember { mutableStateListOf<String>() }
    val looksLikeUrl = query.contains('.') && !query.contains(' ')

    fun addCustom() {
        if (query.isBlank() || busy) return
        busy = true; err = null
        scope.launch {
            val r = vm.addSource(query)
            busy = false
            if (r == null) query = "" else err = r
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.closeSources() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Sources", fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.showWizard = true }) { Text("Interests") }
        }
        LazyColumn(
            Modifier.fillMaxSize().widthIn(max = 760.dp).align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it; err = null },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Search ${Catalog.sources.size} sources or paste a website") },
                    isError = err != null, supportingText = { err?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (looksLikeUrl) addCustom() }),
                    trailingIcon = {
                        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else if (looksLikeUrl) IconButton(onClick = { addCustom() }) { Icon(Icons.Default.Add, "Add") }
                    },
                )
                if (looksLikeUrl) Text("Tap + to add this site's feed", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                Spacer(Modifier.height(12.dp))
            }

            if (query.length >= 2 && !looksLikeUrl) {
                val hits = Catalog.search(query)
                if (hits.isEmpty()) item { Text("No matches. Paste a website address to add it.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(hits, key = { "q-" + it.id }) { c ->
                    SourceRow(c, vm.hasUrl(c.url)) { on -> if (on) vm.addCatalog(c) else vm.removeUrl(c.url) }
                }
            } else {
                // ---- yours
                item {
                    Row(Modifier.fillMaxWidth().clickable { showMine = !showMine }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Your sources", fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        Text("${vm.sources.count { it.enabled }} on · ${vm.sources.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Icon(if (showMine) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                    }
                }
                if (showMine) items(vm.sources, key = { "m-" + it.id }) { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.url, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(checked = s.enabled, onCheckedChange = { vm.toggleSource(s.id) })
                        IconButton(onClick = { vm.removeSource(s.id) }) { Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                }

                // ---- browse
                item {
                    Spacer(Modifier.height(22.dp))
                    Text("Browse by topic", fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                }
                Catalog.topics.forEach { t ->
                    val all = Catalog.forTopic(t.id)
                    val isOpen = t.id in open
                    item(key = "t-" + t.id) {
                        Row(
                            Modifier.fillMaxWidth().clickable { if (isOpen) open.remove(t.id) else open.add(t.id) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(t.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Text(t.blurb, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                            Text("${all.count { vm.hasUrl(it.url) }}/${all.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Icon(if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    if (isOpen) items(all, key = { "c-" + it.id }) { c ->
                        SourceRow(c, vm.hasUrl(c.url)) { on -> if (on) vm.addCatalog(c) else vm.removeUrl(c.url) }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
