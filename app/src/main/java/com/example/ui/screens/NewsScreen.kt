package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.platform.ShareService
import com.example.ui.F1ViewModel
import com.example.ui.components.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(vm: F1ViewModel, onArticle: (String) -> Unit) {
    val news by vm.news.collectAsStateWithLifecycle()
    val error by vm.newsError.collectAsStateWithLifecycle()
    val read by vm.readIds.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("For you") }
    var limit by rememberSaveable { mutableIntStateOf(3) }
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) { if (refreshing) { vm.refreshNews(); delay(400); refreshing = false } }
    val filtered = news.filter { article ->
        (query.isBlank() || (article.title + article.summary).contains(query, ignoreCase = true)) && when (filter) {
            "Saved" -> article.isBookmarked
            "Technical" -> article.category == "TECHNICAL"
            "Weekend" -> article.category == "RACE WEEKEND" || article.category == "ANALYSIS"
            else -> true
        }
    }
    PullToRefreshBox(refreshing, { refreshing = true }, Modifier.fillMaxSize()) {
        ApexLazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Eyebrow("BEYOND THE CHEQUERED FLAG", Coral)
                Text("The paddock journal.", fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                Spacer(Modifier.height(6.dp))
                Text("The stories behind every second.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            item {
                OutlinedTextField(query, { query = it; limit = 3 }, Modifier.fillMaxWidth(), placeholder = { Text("Search the journal", fontSize = 13.sp) },
                    leadingIcon = { Glyph("search", size = 19.dp) }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                ApexLazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("For you", "Weekend", "Technical", "Saved")) { category ->
                        FilterChip(selected = filter == category, onClick = { filter = category; limit = 3 }, label = { Text(category, fontSize = 12.sp) })
                    }
                }
            }
            if (error) item { EmptyState("The journal couldn't load", "Your saved articles stay on this device.", "Retry", vm::refreshNews) }
            else if (news.isEmpty()) items(3) { Box(Modifier.fillMaxWidth().height(115.dp).clip(PanelShape).background(MaterialTheme.colorScheme.surfaceVariant)) }
            else if (filtered.isEmpty()) item { EmptyState(if (filter == "Saved") "Your reading list starts here" else "No matching stories", if (filter == "Saved") "Bookmark a story to keep it for later." else "Try another search or category.") }
            items(filtered.take(limit), key = { it.id }) { article ->
                val featured = article.id == "apex-editorial-1" && filter != "Saved"
                Column(Modifier.fillMaxWidth().clip(PanelShape).background(MaterialTheme.colorScheme.surface)) {
                    Box(Modifier.fillMaxWidth().height(if (featured) 200.dp else 112.dp).clickable { onArticle(article.id) }) {
                        RacingArtwork(article.category, Modifier.fillMaxSize())
                        BadgeWrapper(Modifier.align(Alignment.TopStart).padding(13.dp), if (featured) "THE WEEKEND EDIT" else article.category)
                        if (featured) Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                            Text("THE STREETS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-.8).sp)
                            Text("DON'T FORGIVE.", color = Color(0xFFE49AA8), fontSize = 28.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-.8).sp)
                        }
                    }
                    Column(Modifier.padding(start = 16.dp, top = 15.dp, end = 10.dp, bottom = 8.dp)) {
                        Text(article.title, Modifier.clickable { onArticle(article.id) }, fontSize = if (featured) 22.sp else 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.5).sp)
                        Spacer(Modifier.height(7.dp))
                        Text(article.summary, fontSize = 12.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("APEX · DEMO EDITORIAL", Modifier.weight(1f), fontSize = 8.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (article.id in read) Text("Read", fontSize = 9.sp, color = Mint)
                            ActionIcon("bookmark", if (article.isBookmarked) "Remove saved article" else "Save article", { vm.toggleNewsBookmark(article) }, if (article.isBookmarked) Coral else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (filtered.size > limit) item { OutlinedButton(onClick = { limit += 3 }, Modifier.fillMaxWidth()) { Text("More stories") } }
            item { InfoNote("Original demonstration articles, stored offline. Current news is not available in this demo. Your saved stories remain on this device.") }
        }
    }
}

@Composable
private fun BadgeWrapper(modifier: Modifier, text: String) { Box(modifier) { Badge(text, Color.White) } }

@Composable
fun RacingArtwork(category: String, modifier: Modifier = Modifier) {
    val accent = if (category == "TECHNICAL") Mint else Color(0xFFE49AA8)
    Canvas(modifier) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF3A232D), Color(0xFF17131A), Color(0xFF30252E))))
        for (i in 0..8) {
            val y = size.height * i / 6
            drawLine(Color.White.copy(alpha=.05f),Offset(0f,y),Offset(size.width,y-size.height*.65f),1f)
        }
        val track = Path().apply { moveTo(size.width * -.1f,size.height*.2f); cubicTo(size.width*.3f,-size.height*.3f,size.width*.2f,size.height*1.4f,size.width*1.15f,size.height*.4f) }
        drawPath(track, Color.White.copy(alpha=.06f), style=Stroke(size.height*.4f))
        drawPath(track, accent.copy(alpha=.2f), style=Stroke(1.5f))
        withTransform({ translate(size.width * .72f, size.height * .51f); rotate(36f,Offset.Zero); scale(size.height/190f,size.height/190f,Offset.Zero) }) {
            drawOval(Color.Black.copy(alpha=.35f),Offset(-52f,-92f),Size(104f,190f))
            for (y in listOf(-60f,53f)) for (x in listOf(-42f,24f)) drawRoundRect(Color(0xFF06070A),Offset(x,y),Size(19f,37f),CornerRadius(5f))
            drawRoundRect(accent,Offset(-40f,-60f),Size(80f,9f),CornerRadius(2f))
            drawRoundRect(accent.copy(alpha=.6f),Offset(-37f,79f),Size(74f,10f),CornerRadius(2f))
            val body = Path().apply { moveTo(-4f,-82f); lineTo(4f,-82f); lineTo(11f,-15f); lineTo(29f,11f); lineTo(24f,57f); lineTo(11f,71f); lineTo(-11f,71f); lineTo(-24f,57f); lineTo(-29f,11f); lineTo(-11f,-15f); close() }
            drawPath(body,Brush.horizontalGradient(listOf(accent.copy(alpha=.5f),accent,accent.copy(alpha=.5f)),-29f,29f))
            drawOval(Color(0xFF11131A),Offset(-10f,-9f),Size(20f,35f))
            drawOval(Color(0xFFE3DCD8),Offset(-5f,4f),Size(10f,12f))
            drawLine(Color.White.copy(alpha=.6f),Offset(0f,-68f),Offset(0f,-20f),2f)
        }
    }
}

@Composable
fun ArticleDetail(vm: F1ViewModel, id: String?) {
    val articles by vm.news.collectAsStateWithLifecycle()
    val article = articles.find { it.id == id }
    val context = LocalContext.current
    if (article == null) { EmptyState("Article unavailable", "Return to the journal to choose a story."); return }
    ApexLazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { RacingArtwork(article.category, Modifier.fillMaxWidth().height(180.dp).clip(PanelShape)) }
        item {
            Eyebrow(article.category + " · DEMO EDITORIAL", Coral)
            Spacer(Modifier.height(12.dp))
            Text(article.title, fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp, lineHeight = 35.sp)
            Text("APEX Field Notes · Original sample content", Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Row {
                TextButton(onClick = { vm.toggleNewsBookmark(article) }) { Glyph("bookmark", size = 17.dp); Spacer(Modifier.width(7.dp)); Text(if (article.isBookmarked) "Saved" else "Save for later") }
                TextButton(onClick = { if (!ShareService.text(context, article.title, "APEX demo editorial: ${article.title}\napex://article/${article.id}")) vm.message("Sharing is unavailable.") }) { Glyph("share", size = 17.dp); Spacer(Modifier.width(7.dp)); Text("Share") }
            }
        }
        items(article.content.split("\n\n")) { paragraph ->
            Text(paragraph, fontSize = if (paragraph.length < 40) 12.sp else 15.sp, fontWeight = if (paragraph.length < 40) FontWeight.Bold else FontWeight.Normal,
                lineHeight = 25.sp, color = if (paragraph.length < 40) Coral else MaterialTheme.colorScheme.onSurface)
        }
    }
}
