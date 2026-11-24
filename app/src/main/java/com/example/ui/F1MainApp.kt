package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.F1Red
import com.example.ui.explore.ExploreScreen
import com.example.data.explore.Compound
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun F1MainApp(viewModel: F1ViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var articleId by rememberSaveable { mutableStateOf<String?>(null) }
    var eventId by rememberSaveable { mutableStateOf<String?>(null) }
    val driverId by viewModel.selectedDriver.collectAsStateWithLifecycle()
    val experience by viewModel.experience.collectAsStateWithLifecycle()
    val link by viewModel.deepLink.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val destinations = listOf("Live", "News", "Fantasy", "Community", "Profile")
    val screenState = rememberSaveableStateHolder()
    LaunchedEffect(viewModel) { viewModel.feedback.collect { snack.showSnackbar(it) } }
    LaunchedEffect(experience) { if(experience!=null)sheet=null }
    LaunchedEffect(link) {
        link?.let {
            val uri = android.net.Uri.parse(it)
            if(uri.host!="explore")viewModel.openExperience(null)
            when (uri.host) {
                "race" -> { tab = 0; eventId = uri.lastPathSegment; sheet = "events" }
                "driver" -> { uri.lastPathSegment?.let(viewModel::selectDriver); sheet = "driver" }
                "article" -> { tab = 1; articleId = uri.lastPathSegment; sheet = "article" }
                "fantasy" -> { tab = 2; if (uri.pathSegments.firstOrNull() == "league") uri.lastPathSegment?.let(viewModel::joinLeague) }
                "explore" -> {
                    val route=uri.lastPathSegment?:"hub"
                    if(route=="strategy") {
                        val old=viewModel.strategyPlan
                        val start=runCatching {Compound.valueOf(uri.getQueryParameter("start").orEmpty())}.getOrDefault(old.start)
                        val next=runCatching {Compound.valueOf(uri.getQueryParameter("next").orEmpty())}.getOrDefault(old.next)
                        val loss=uri.getQueryParameter("loss")?.toDoubleOrNull()?.takeIf {it.isFinite()&&it in 5.0..60.0}?:old.pitLossSeconds
                        val wear=uri.getQueryParameter("wear")?.toDoubleOrNull()?.takeIf {it.isFinite()&&it in .25..3.0}?:old.degradationFactor
                        val safety=uri.getQueryParameter("sc")?.let {when(it){"true"->true;"false"->false;else->old.safetyCar}}?:old.safetyCar
                        val plan=old.copy(pitLap=uri.getQueryParameter("pit")?.toIntOrNull()?.coerceIn(5,53)?:old.pitLap,start=start,next=next,rainPercent=uri.getQueryParameter("rain")?.toIntOrNull()?.coerceIn(0,100)?:old.rainPercent,pitLossSeconds=loss,degradationFactor=wear,safetyCar=safety)
                        viewModel.updateStrategy(plan)
                    }
                    if(route=="replay"&&uri.getQueryParameter("mode") in listOf("demo","recorded"))viewModel.requestReplayAt(uri.getQueryParameter("t")?.toLongOrNull()?:0L,uri.getQueryParameter("mode")=="recorded")
                    viewModel.openExperience(route)
                }
            }
            viewModel.consumeDeepLink()
        }
    }
    BackHandler(sheet != null || experience!=null || tab != 0) {
        if(sheet!=null)sheet=null
        else if(experience!=null)viewModel.openExperience(if(experience=="hub")null else "hub")
        else tab=0
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 21.dp, end = 12.dp).height(61.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if(experience!=null)ActionIcon("back","Back from experience",{viewModel.openExperience(if(experience=="hub")null else "hub")})else BrandMark()
                Text(ApexBrand.name, Modifier.padding(start = 6.dp), fontSize = 25.sp, fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                if(experience==null)TextButton(onClick={viewModel.openExperience("hub")}){Text("Explore",fontSize=12.sp,color=Coral)}
                Badge("DEMO", Coral)
                Spacer(Modifier.width(8.dp))
                ActionIcon("bell", "Notification preferences", { sheet = "notifications" })
            }
        },
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = .5.dp)
                Row(Modifier.fillMaxWidth().height(67.dp).padding(horizontal = 10.dp)) {
                    destinations.forEachIndexed { index, title ->
                        val active = index == tab
                        Column(Modifier.weight(1f).fillMaxHeight().semantics { selected = active; contentDescription = title + " tab" }
                            .clickable { viewModel.openExperience(null);tab = index }.padding(top = 9.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.width(16.dp).height(2.dp).background(if (active) F1Red else Color.Transparent, RoundedCornerShape(2.dp)))
                            Glyph(title, color = if (active) F1Red else MaterialTheme.colorScheme.onSurfaceVariant, size = 21.dp)
                            Text(title, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (tab != 0 && experience==null) {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable { tab = 0 }
                    .padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(Coral, RoundedCornerShape(3.dp)))
                    Text("Monaco GP", Modifier.padding(start = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("  /  Demo replay", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Glyph("arrow", size = 14.dp)
                }
            }
            Crossfade(targetState = experience?:"tab-$tab", modifier = Modifier.weight(1f), animationSpec = tween(if (viewModel.reducedMotion) 0 else 180), label = "Navigation") { destination ->
            screenState.SaveableStateProvider(destination) {
            if(!destination.startsWith("tab-"))ExploreScreen(viewModel,destination)
            else when (destination.removePrefix("tab-").toIntOrNull()) {
                0 -> LiveScreen(viewModel,
                    onDriver = { viewModel.selectDriver(it); sheet = "telemetry" },
                    onProfile = { viewModel.selectDriver(it); sheet = "driver" },
                    onEvents = { sheet = "events" })
                1 -> NewsScreen(viewModel) { articleId = it; viewModel.markRead(it); sheet = "article" }
                2 -> FantasyScreen(viewModel)
                3 -> CommunityScreen(viewModel)
                4 -> ProfileScreen(viewModel,
                    onDriver = { viewModel.selectDriver(it); sheet = "driver" },
                    onNotifications = { sheet = "notifications" })
            }
            }
            }
        }
    }
    if (sheet != null) {
        ModalBottomSheet(onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow(when (sheet) { "telemetry" -> "DRIVER TELEMETRY"; "driver" -> "DRIVER PROFILE"; "events" -> "RACE CONTROL"; "article" -> "THE PADDOCK JOURNAL"; else -> "NOTIFICATIONS" })
                    Spacer(Modifier.weight(1f))
                    ActionIcon("close", "Close details", { sheet = null })
                }
                when (sheet) {
                    "telemetry" -> TelemetryDetail(viewModel, driverId, onProfile = { sheet = "driver" })
                    "driver" -> DriverProfile(viewModel, driverId, onTelemetry = { sheet = "telemetry"; tab = 0 })
                    "events" -> RaceControlScreen(viewModel, eventId)
                    "article" -> ArticleDetail(viewModel, articleId)
                    "notifications" -> NotificationPreferencesScreen(viewModel)
                }
            }
        }
    }
}
