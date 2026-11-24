package com.example.ui.explore

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.race3d.RaceScreen
import com.example.data.explore.*
import com.example.ui.DriverDoodle
import com.example.ui.F1ViewModel
import com.example.ui.components.*
import com.example.ui.screens.CommunityScreen
import com.example.ui.theme.F1Red
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class Experience(val route:String,val name:String,val description:String)
val Experiences=listOf(
    Experience("pals","Paddock Pals","Little drivers. Big personalities."),
    Experience("helmets","Helmet Vault","A collector’s eye. A new perspective."),
    Experience("pitwall","AI Pit Wall","Follow the evidence behind a decision."),
    Experience("replay","Race Time Machine","Take control of the race clock."),
    Experience("strategy","Strategy Simulator","One different call. Another outcome."),
    Experience("garage","Digital Garage","Get closer to the engineering."),
    Experience("rivalry","Rivalry Lab","The margins that separate two drivers."),
    Experience("intelligence","Race Intelligence","Understand the call, as it happens."),
    Experience("paddock","Virtual Paddock","Make a prediction. Find your people."),
    Experience("career","Career Universe","Every season leaves a story.")
)
fun publishedPoints(value:Double):String=java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

@Composable
fun ExploreScreen(vm:F1ViewModel,route:String) {
    if (route == "race3d" || route.startsWith("race3d/")) {
        RaceScreen(vm.circuitCatalog, route.substringAfter('/', "").ifEmpty { null })
        return
    }
    when(route) {
        "circuits"->CircuitAtlasScreen(vm)
        "pals"->PalsScreen(vm)
        "helmets"->HelmetVaultScreen(vm)
        "garage"->GarageScreen(vm)
        "strategy"->StrategyScreen(vm)
        "pitwall"->PitWallScreen(vm)
        "replay"->TimeMachineScreen(vm)
        "rivalry"->RivalryScreen(vm)
        "intelligence"->IntelligenceScreen(vm)
        "career"->CareerScreen(vm)
        "paddock"->VirtualPaddockScreen(vm)
        "standings"->PublishedStandingsScreen(vm)
        else->ExploreHub(vm)
    }
}

@Composable
fun ExploreHeading(kicker:String,title:String,subtitle:String) {
    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Eyebrow(kicker,Coral)
        Text(title,fontSize=31.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1.1).sp,lineHeight=34.sp)
        Text(subtitle,fontSize=12.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExploreHub(vm:F1ViewModel) {
    val featured=vm.circuitCatalog.firstOrNull {it.id=="mc-1929"}
    ApexLazyColumn(Modifier.testTag("explore-feed"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item { ExploreHeading("APEX / BEYOND THE RACE","A little closer.\nA lot more racing.","An interactive world of circuits, characters and race intelligence.") }
        item {
            Column(Modifier.fillMaxWidth().clip(PanelShape).background(Color(0xFF151116)).clickable {vm.openExperience("circuits")}.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Eyebrow("THE 2027 COLLECTION",Color(0xFFE49AA8));Badge("24 CIRCUITS",Color.White)}
                if(featured!=null)CircuitRelief(featured,Modifier.fillMaxWidth().height(165.dp),reducedMotion=vm.reducedMotion)
                else Box(Modifier.fillMaxWidth().height(130.dp),contentAlignment=Alignment.Center){Text("Loading the world tour…",color=Color.White)}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("The world, raised.",Modifier.weight(1f),fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    Glyph("arrow",color=Color(0xFFFF3D4F))
                }
                Text("Explore every circuit in 3D",Modifier.padding(top=7.dp),fontSize=12.sp,color=Color(0xFFB7A4AE))
            }
        }
        item {
            Button(onClick = { vm.openExperience("race3d") }, modifier = Modifier.fillMaxWidth()) {
                Text("Race 3D")
            }
        }
        item {SectionTitle("Your all-access pass")}
        items(Experiences,key={it.route}) { feature ->
            val number=Experiences.indexOf(feature)+1
            Row(Modifier.fillMaxWidth().heightIn(min=82.dp).clip(RoundedCornerShape(12.dp)).clickable {vm.openExperience(feature.route)}.padding(vertical=12.dp,horizontal=7.dp),verticalAlignment=Alignment.CenterVertically) {
                if(feature.route=="pals")DriverDoodle("leclerc",F1Red,Modifier.size(50.dp),vm.reducedMotion,onOpenProfile={vm.openExperience("pals")})
                else Text(number.toString().padStart(2,'0'),Modifier.width(50.dp),fontSize=26.sp,color=Coral,fontStyle=FontStyle.Italic,fontWeight=FontWeight.Light)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text(feature.name,fontSize=18.sp,fontWeight=FontWeight.SemiBold)
                    Text(feature.description,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Glyph("arrow",size=17.dp)
            }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))
        }
        item {OutlinedButton(onClick={vm.openExperience("standings")},Modifier.fillMaxWidth()){Text("Published 2026 standings")}}
        item {InfoNote("Original APEX models and characters. Race feed is a demonstration. Public historical results are fetched only in data views; estimates and what-if results are labeled.")}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitAtlasScreen(vm:F1ViewModel) {
    var query by rememberSaveable {mutableStateOf("")}
    var filter by rememberSaveable {mutableStateOf("All 24")}
    var selected by rememberSaveable {mutableStateOf<String?>(null)}
    val context=LocalContext.current
    val focus=LocalFocusManager.current
    val maxDetailHeight=LocalConfiguration.current.screenHeightDp.dp*.9f
    val circuits=vm.circuitCatalog.filter {(filter!="Sprints"||it.sprint)&&(it.country+it.nation+it.circuit+it.location+if(it.nation=="United States")" USA"else"").contains(query,true)}
    ApexLazyColumn(Modifier.testTag("circuit-collection"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {ExploreHeading("THE 2027 CIRCUIT COLLECTION","The world, raised.","24 rounds. 22 countries. A different challenge at every turn.")}
        item {OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("Find a circuit or country")},singleLine=true,leadingIcon={Glyph("search",size=18.dp)},shape=RoundedCornerShape(12.dp))}
        item {ChoiceTabs(listOf("All 24","Sprints"),filter,{filter=it})}
        if(vm.circuitCatalog.isEmpty())item {
            if(vm.catalogError!=null)EmptyState("Collection unavailable",vm.catalogError.orEmpty())else LinearProgressIndicator(Modifier.fillMaxWidth())
            if(vm.catalogError!=null)OutlinedButton(onClick=vm::loadCircuitCatalog){Text("Retry collection")}
        }
        if(circuits.isEmpty()&&vm.circuitCatalog.isNotEmpty())item {EmptyState("No matching circuits","Try another name or switch to All 24.")}
        items(circuits.chunked(2),key={it.first().id}) { pair ->
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                pair.forEach { c ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClickLabel="Explore ${c.country} in 3D"){focus.clearFocus();selected=c.id}.padding(11.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Eyebrow(c.round.toString().padStart(2,'0'));if(c.sprint)Text("SPRINT",fontSize=8.sp,color=Coral)}
                        CircuitRelief(c,Modifier.fillMaxWidth().height(107.dp),reducedMotion=vm.reducedMotion)
                        Text(c.country,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1,modifier=Modifier.padding(top=9.dp))
                        Text(c.weekend,fontSize=9.sp,color=Coral,modifier=Modifier.padding(top=4.dp))
                        Text(c.location,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))
                    }
                }
                if(pair.size==1)Spacer(Modifier.weight(1f))
            }
        }
        item {InfoNote("Calendar published by Formula 1 on 16 September 2026. Türkiye is subject to circuit homologation. Geographic footprints: Tomislav Bacinger, MIT. Layouts may change. Raised depth is artistic, not terrain elevation.")}
        item {TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(CircuitCatalog.CALENDAR_SOURCE)))}){Text("Official calendar & dates ↗")}}
    }
    val circuit=vm.circuitCatalog.find {it.id==selected}
    if(circuit!=null)ModalBottomSheet(onDismissRequest={selected=null},containerColor=MaterialTheme.colorScheme.background,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().heightIn(max=maxDetailHeight).verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Eyebrow("ROUND ${circuit.round} / 2027",Coral,Modifier.weight(1f));ActionIcon("close","Close circuit",{selected=null})}
            Text(circuit.country,fontSize=30.sp,fontWeight=FontWeight.Bold)
            Text(circuit.circuit+" · "+circuit.nation,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            CircuitRelief(circuit,Modifier.fillMaxWidth().height(300.dp),interactive=true,reducedMotion=vm.reducedMotion)
            Button(
                onClick = {
                    selected = null
                    vm.openExperience("race3d/${circuit.id}")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Race this circuit") }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("RACE WEEKEND",circuit.weekend);if(circuit.sprint)Badge("SPRINT",Coral)}
            Text("Drag to rotate and tilt · Pinch to zoom",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(circuit.homologationPending)Badge("SUBJECT TO FIA HOMOLOGATION",Amber)
            InfoNote("Geographic outline, simplified for display. The 3D extrusion is decorative. No vehicle positions or elevation measurements are implied.")
        }
    }
}

@Composable
fun DriverPicker(vm:F1ViewModel,selected:String,onSelect:(String)->Unit) {
    ApexLazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {items(vm.drivers,key={it.id}) {d->FilterChip(selected==d.id,{onSelect(d.id)},label={Text(d.code)})}}
}

@Composable
private fun HelmetVaultScreen(vm:F1ViewModel) {
    var driverId by rememberSaveable {mutableStateOf(vm.selectedDriver.value)}
    var season by rememberSaveable {mutableIntStateOf(2026)}
    var open by rememberSaveable {mutableStateOf(false)}
    var exploded by rememberSaveable {mutableStateOf(false)}
    var part by rememberSaveable {mutableStateOf("All")}
    var lighting by rememberSaveable {mutableStateOf("Studio")}
    val driver=vm.drivers.first {it.id==driverId}
    val faces=remember(driverId,season,open,exploded) {MeshFactory.helmet(driver.color,season,open,exploded)}
    ApexLazyColumn(Modifier.testTag("helmet-vault"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {ExploreHeading("APEX / COLLECTOR’S ARCHIVE","The Helmet Vault.","A closer look at the object behind the identity.")}
        item {DriverPicker(vm,driverId,{driverId=it})}
        item {
            Box {
                MeshViewer("helmet",faces,Modifier.fillMaxWidth().height(330.dp),selectedPart=if(part=="All")null else part.lowercase(),lighting=lighting,reducedMotion=vm.reducedMotion)
                Column(Modifier.align(Alignment.TopStart).padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {Eyebrow("CONCEPT / $season",Color(0xFFE49AA8));Text(driver.code,color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold)}
                BadgeInCorner("ORIGINAL 3D MODEL")
            }
        }
        item {ChoiceTabs(listOf("2024","2025","2026"),season.toString(),{season=it.toInt()})}
        item {Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {OutlinedButton(onClick={open=!open},Modifier.weight(1f)){Text(if(open)"Close visor" else "Open visor")};OutlinedButton(onClick={exploded=!exploded},Modifier.weight(1f)){Text(if(exploded)"Assemble" else "Explode layers")}}}
        item {Eyebrow("INSPECT A LAYER");Spacer(Modifier.height(8.dp));ChoiceTabs(listOf("All","Shell","Visor","Liner"),part,{part=it})}
        item {ChoiceTabs(listOf("Studio","Warm","Night"),lighting,{lighting=it})}
        item {Panel {Eyebrow("${driver.name.uppercase()} / APEX CONCEPT",Coral);Text(when(part){"Shell"->"The shell distributes impact loads. This model illustrates its form; it is not a certified safety product.";"Visor"->"The visor protects the eye opening. Rotate the model, open it or separate it from the shell.";"Liner"->"An energy-absorbing liner sits inside the shell. The dark lower layer is a simplified cutaway.";else->"Three original APEX color studies, inspired by your selected driver’s palette. Rotate, zoom and inspect the separate layers."},fontSize=13.sp,lineHeight=21.sp);InfoNote("Fictional season-inspired liveries. These are not replicas, race-worn helmets or a verified historical collection. Licensed models can replace these procedural assets.")}}
    }
}

@Composable
private fun BoxScope.BadgeInCorner(text:String) {Box(Modifier.align(Alignment.TopEnd).padding(14.dp)){Badge(text,Color(0xFFB8B0BC))}}

@Composable
private fun GarageScreen(vm:F1ViewModel) {
    var exploded by rememberSaveable {mutableStateOf(false)}
    var airflow by rememberSaveable {mutableStateOf(false)}
    var component by rememberSaveable {mutableStateOf("All")}
    var color by rememberSaveable {mutableStateOf("Rosso")}
    val paint=when(color){"Glacier"->Color(0xFF48C1C5);"Papaya"->Color(0xFFFF8C32);else->Color(0xFFE82749)}
    val faces=remember(exploded,color){MeshFactory.car(paint,exploded)}
    val parts=listOf("All","Front wing","Rear wing","Power unit","Floor","Halo","Tyres")
    ApexLazyColumn(Modifier.testTag("digital-garage"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {ExploreHeading("APEX ENGINEERING / C–01","Inside the machine.","Your own turntable. Every angle, another detail.")}
        item {
            Box {
                MeshViewer("race car",faces,Modifier.fillMaxWidth().height(310.dp),selectedPart=if(component=="All")null else component,stage=true,reducedMotion=vm.reducedMotion,airflow=airflow)
                Column(Modifier.align(Alignment.TopStart).padding(15.dp)) {Eyebrow("APEX",Color(0xFFE49AA8));Text("C–01",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Black)}
                Column(Modifier.align(Alignment.TopEnd).padding(15.dp),horizontalAlignment=Alignment.End) {Eyebrow("DESIGN STUDY",Color(0xFFB8B0BC));Text("2026 ERA",fontSize=10.sp,color=Color.White,modifier=Modifier.padding(top=5.dp))}
            }
        }
        item {ChoiceTabs(listOf("Rosso","Glacier","Papaya"),color,{color=it})}
        item {Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){OutlinedButton(onClick={exploded=!exploded},Modifier.weight(1f)){Text(if(exploded)"Assemble car" else "Explode car")};OutlinedButton(onClick={airflow=!airflow},Modifier.weight(1f)){Text(if(airflow)"Hide airflow" else "Show airflow")}}}
        item {ApexLazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(parts){p->FilterChip(component==p,{component=p},label={Text(p)})}}}
        item {Panel {Eyebrow("ENGINEERING NOTE",Coral);Text(component,fontSize=23.sp,fontWeight=FontWeight.Bold);Text(when(component){"Front wing","Rear wing"->"Active aerodynamics move front and rear wing elements for different drag and downforce requirements in the 2026 rules. This simplified concept does not reproduce a team’s design.";"Power unit"->"The 2026 hybrid power unit combines combustion and electrical propulsion. Energy harvesting and deployment affect the race as much as outright power.";"Floor"->"The floor shapes airflow underneath the car. Its interaction with ride height is important; this geometry is educational, not aerodynamic analysis.";"Halo"->"The halo is a cockpit protection structure. The model highlights its location, not its certified dimensions.";"Tyres"->"Compound, temperature, pressure and tyre age influence grip. The decorative wheels here contain no measured tyre data.";else->"Rotate the car with a horizontal drag. Pinch to zoom and tilt. Select a component to isolate it, then explode the assembly."},fontSize=13.sp,lineHeight=22.sp);InfoNote("Original low-poly model. Airflow lines are illustrations, not CFD. No team logos, sponsor assets, engineering dimensions or performance claims are copied from the reference video.")}}
    }
}

@Composable
private fun PalsScreen(vm:F1ViewModel) {
    var id by rememberSaveable {mutableStateOf(vm.selectedDriver.value)}
    var pose by rememberSaveable {mutableStateOf("Idle")}
    var trigger by remember {mutableIntStateOf(0)}
    val driver=vm.drivers.first {it.id==id}
    val other=vm.drivers.first {it.id!=id}
    val scope=rememberCoroutineScope();val progress=remember {Animatable(0f)};val rival=remember {Animatable(0f)}
    var racing by remember {mutableStateOf(false)};var winner by rememberSaveable {mutableStateOf("")}
    ApexLazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {ExploreHeading("APEX PADDOCK PALS","Small drivers.\nBig personalities.","Tap to jump. Drag to play. Hold to open the driver’s profile.")}
        item {DriverPicker(vm,id,{id=it;pose="Idle";winner=""})}
        item {Box(Modifier.fillMaxWidth().height(220.dp).clip(PanelShape).background(MaterialTheme.colorScheme.surface),contentAlignment=Alignment.Center){
            Text(driver.number.toString(),Modifier.align(Alignment.TopEnd).padding(16.dp),fontSize=92.sp,fontWeight=FontWeight.Black,color=driver.color.copy(alpha=.1f))
            DriverDoodle(driver.code,driver.color,Modifier.size(170.dp),vm.reducedMotion,pose,draggable=true,trigger=trigger,onOpenProfile={vm.selectDriver(id);vm.openExperience("career")})
            Text(driver.name,Modifier.align(Alignment.BottomStart).padding(16.dp),fontSize=17.sp,fontWeight=FontWeight.SemiBold)
        }}
        item {ApexLazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(listOf("Idle","Celebrate","Disappointed","Adjust helmet","Arrive","Exit")){p->FilterChip(pose==p,{pose=p;trigger++},label={Text(p)})}}}
        item {
            Panel {
                Eyebrow("THE PADDOCK SPRINT / PLAY MODE",Coral)
                BoxWithConstraints(Modifier.fillMaxWidth().height(140.dp)) {
                    val trackWidth=maxWidth-62.dp
                    Box(Modifier.offset(y=64.dp).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    DriverDoodle(driver.code,driver.color,Modifier.offset(x=trackWidth*progress.value).size(62.dp),vm.reducedMotion,if(racing)"Race"else"Idle",trigger=trigger)
                    DriverDoodle(other.code,other.color,Modifier.offset(x=trackWidth*rival.value,y=70.dp).size(62.dp),vm.reducedMotion,if(racing)"Race"else"Idle",trigger=trigger)
                }
                if(winner.isNotEmpty())Text(winner,fontSize=14.sp,color=Mint)
                Button(onClick={
                    scope.launch {
                        racing=true;winner="";trigger++;pose="Race";progress.snapTo(0f);rival.snapTo(0f)
                        val a=1800+kotlin.math.abs(driver.id.hashCode()%650);val b=1800+kotlin.math.abs(other.id.hashCode()%650)
                        kotlinx.coroutines.coroutineScope {launch {progress.animateTo(1f,tween(if(vm.reducedMotion)0 else a))};launch {rival.animateTo(1f,tween(if(vm.reducedMotion)0 else b))}}
                        winner=(if(a<=b)driver.code else other.code)+" wins the playful sprint!";racing=false;pose="Celebrate"
                    }
                },enabled=!racing,modifier=Modifier.fillMaxWidth()){Text(if(racing)"Racing…"else"Race ${other.code}")}
                InfoNote("Toy race with deterministic character speeds. This is not a prediction, real result or fantasy scoring event. The sample roster is independent of published 2026 standings.")
            }
        }
    }
}

@Composable
fun SourceStamp(source:String,fetchedAt:Long,cached:Boolean=false,stale:Boolean=false,simulated:Boolean=false) {
    val context=LocalContext.current
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(when{simulated->"SIMULATED DATA · EXHIBITION";stale->"STALE CACHE · LAST SUCCESSFUL FETCH";cached->"CACHED · PUBLISHED DATA";else->"PUBLISHED DATA · OBSERVED"},fontSize=9.sp,fontWeight=FontWeight.Bold,color=if(stale||simulated)Amber else Mint,letterSpacing=.7.sp)
        Text(SimpleDateFormat("dd MMM yyyy · HH:mm 'UTC'",Locale.US).apply {timeZone=TimeZone.getTimeZone("UTC")}.format(Date(fetchedAt)),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(source.startsWith("https://"))TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(source)))},contentPadding=PaddingValues(0.dp)){Text("View data source ↗",fontSize=11.sp)}else Text(source,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun <T> DataLoading(state:LoadState<T>,onRetry:()->Unit) {
    if(state.loading)Column(verticalArrangement=Arrangement.spacedBy(8.dp)){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Loading published data…",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    if(state.error!=null)Panel {Text("Data unavailable",fontWeight=FontWeight.Bold);Text(state.error,fontSize=12.sp);OutlinedButton(onClick=onRetry){Text("Retry")}}
}

@Composable
private fun PublishedStandingsScreen(vm:F1ViewModel) {
    LaunchedEffect(Unit){vm.loadVerifiedStandings()}
    val state=vm.verifiedStandings
    ApexLazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
        item {ExploreHeading("THE CHAMPIONSHIP / 2026","Every point matters.","Published standings from Jolpica. This view is separate from the exhibition race.")}
        item {DataLoading(state,{vm.loadVerifiedStandings(true)})}
        val snapshot=state.snapshot
        if(snapshot!=null) {
            item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("After round ${snapshot.data.round}",Modifier.weight(1f),fontSize=20.sp,fontWeight=FontWeight.Bold);TextButton(onClick={vm.loadVerifiedStandings(true)},enabled=!state.loading){Text("Refresh")}};SourceStamp(snapshot.source,snapshot.fetchedAt,snapshot.cached,snapshot.stale)}
            items(snapshot.data.rows,key={it.id}) { row ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable {vm.archiveDriver=row;vm.openExperience("career")}.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(row.position.toString().padStart(2,'0'),Modifier.width(32.dp),fontSize=17.sp,color=Coral)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(row.name,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text(row.team,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    Text(publishedPoints(row.points),fontSize=19.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
        item {InfoNote("Points are preserved exactly as supplied, including fractional totals. Updates depend on publication by the provider. No live championship position is inferred.")}
    }
}

@Composable
private fun VirtualPaddockScreen(vm:F1ViewModel) {
    var choice by rememberSaveable {mutableStateOf(vm.predictionVote("next_compound").orEmpty())}
    var room by rememberSaveable {mutableStateOf("Predictions")}
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal=20.dp,vertical=12.dp)) {ExploreHeading("APEX / VIRTUAL PADDOCK","Your race. Your people.","A local fan-room preview. Global accounts and voice need a configured backend.");Spacer(Modifier.height(12.dp));ChoiceTabs(listOf("Predictions","Race room"),room,{room=it})}
        if(room=="Race room")CommunityScreen(vm)else ApexLazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {Panel {Badge("LOCAL PREDICTION",Coral);Text("What would you fit next?",fontSize=25.sp,fontWeight=FontWeight.Bold);Text("Choose a tyre for your hypothetical next stint. No race result or community vote count is implied.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);ChoiceTabs(listOf("Soft","Medium","Hard"),choice,{choice=it;vm.savePrediction("next_compound",it)});if(choice.isNotEmpty())Text("Your pick: $choice · saved on this device",fontSize=12.sp,color=Mint)}}
            item {OutlinedButton(onClick={vm.openExperience("strategy")},Modifier.fillMaxWidth()){Text("Test your call in Strategy Simulator")}}
            item {InfoNote("No fabricated participants or consensus. Text rooms use the existing local community. Voice previews never enable a microphone or claim a live audio connection.")}
        }
    }
}
