package dev.folio.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.folio.news.AppViewModel
import dev.folio.news.data.Catalog
import dev.folio.news.data.CatalogSource

private val Serif = FontFamily.Serif

/** First-run wizard (also reachable from Settings): 1) pick interests, 2) tick the sources you want. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(vm: AppViewModel) {
    var step by remember { mutableIntStateOf(0) }
    var topics by remember { mutableStateOf(vm.interestTopics()) }
    var chosen by remember {
        mutableStateOf(vm.sources.map { it.url }.filter { Catalog.topicOfUrl(it) != null }.toSet())
    }
    val expanded = remember { mutableStateListOf<String>() }

    fun toggleTopic(id: String) {
        if (id in topics) {
            topics = topics - id
            val drop = Catalog.forTopic(id).map { it.url }.toSet()
            chosen = chosen - drop
        } else {
            topics = topics + id
            chosen = chosen + Catalog.featured(listOf(id)).map { it.url }
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Folio", fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Spacer(Modifier.width(6.dp)); Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.weight(1f))
            repeat(2) { i ->
                Box(Modifier.padding(horizontal = 3.dp).size(if (i == step) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                    .background(if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
            }
            if (vm.onboarded) IconButton(onClick = { vm.showWizard = false }) { Icon(Icons.Default.Close, "Close") }
        }

        Column(Modifier.weight(1f).widthIn(max = 760.dp).align(Alignment.CenterHorizontally).padding(horizontal = 24.dp)) {
            if (step == 0) {
                Text("What are you into?", fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("Pick at least three. Folio builds your feed from them and keeps learning from what you read.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, lineHeight = 23.sp)
                Spacer(Modifier.height(22.dp))
                FlowRow(
                    Modifier.verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Catalog.topics.forEach { t ->
                        FilterChip(
                            selected = t.id in topics, onClick = { toggleTopic(t.id) },
                            label = { Text(t.title, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            } else {
                Text("Choose your sources", fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                Spacer(Modifier.height(6.dp))
                Text("We ticked the best ones. Add or remove anything.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.fillMaxSize()) {
                    Catalog.topics.filter { it.id in topics }.forEach { t ->
                        val all = Catalog.forTopic(t.id)
                        val open = t.id in expanded
                        val shown = if (open) all else all.take(7)
                        item(key = "h-" + t.id) {
                            Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(t.title, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                                Text("${all.count { it.url in chosen }} of ${all.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                        items(shown, key = { "r-" + it.id }) { c ->
                            SourceRow(c, c.url in chosen) { on -> chosen = if (on) chosen + c.url else chosen - c.url }
                        }
                        if (all.size > 7) item(key = "m-" + t.id) {
                            TextButton(onClick = { if (open) expanded.remove(t.id) else expanded.add(t.id) }) {
                                Text(if (open) "Show fewer" else "Show ${all.size - 7} more")
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step == 1) TextButton(onClick = { step = 0 }) { Text("Back") }
            Spacer(Modifier.weight(1f))
            if (step == 0) {
                Text("${topics.size} selected", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
                Button(onClick = { step = 1 }, enabled = topics.size >= 3) { Text("Next") }
            } else {
                Text("${chosen.size} sources", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
                Button(
                    onClick = {
                        val byUrl = Catalog.sources.associateBy { it.url }
                        vm.finishOnboarding(topics, chosen.mapNotNull { byUrl[it] })
                    },
                    enabled = chosen.isNotEmpty(),
                ) { Text("Build my Folio") }
            }
        }
    }
}

@Composable
fun SourceRow(c: CatalogSource, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, onCheckedChange = onChange)
        Column(Modifier.weight(1f)) {
            Text(c.name, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (c.isFlipboard) "Flipboard topic feed" else java.net.URI(c.url).host?.removePrefix("www.") ?: "",
                fontSize = 12.sp, maxLines = 1,
                color = if (c.isFlipboard) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
