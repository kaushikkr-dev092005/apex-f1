package com.example.ui.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.explore.*
import com.example.data.race.*
import com.example.platform.ShareService
import com.example.platform.MomentKind
import com.example.ui.DriverDoodle
import com.example.ui.F1ViewModel
import com.example.ui.components.*
import com.example.ui.theme.F1Red
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale
import kotlin.math.*

@Composable
fun StrategyScreen(vm:F1ViewModel) {
    val p=vm.strategyPlan
    val error=remember(p){StrategyEngine.validate(p)}
    val result=remember(p){if(error==null)StrategyEngine.simulate(p)else null}
    val context=LocalContext.current
    ApexLazyColumn(Modifier.testTag("strategy-lab"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {ExploreHeading("APEX / STRATEGY LAB","What if you called it?","An educational 58-lap race model. Change a call and compare it with the baseline.")}
        item {Badge("HYPOTHETICAL · NOT A PREDICTION",Amber)}
        item {Panel {
            Eyebrow("PLANNED PIT STOP",Coral)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom){Text("LAP ${p.pitLap}",Modifier.weight(1f),fontSize=39.sp,fontWeight=FontWeight.Light);Text("OF ${p.laps}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Slider(p.pitLap.toFloat(),{vm.updateStrategy(p.copy(pitLap=it.roundToInt()))},valueRange=5f..53f,steps=47,modifier=Modifier.semantics {contentDescription="Planned pit lap"})
            Eyebrow("STARTING COMPOUND");ChoiceTabs(Compound.entries.map {it.name},p.start.name,{vm.updateStrategy(p.copy(start=Compound.valueOf(it)))})
            Eyebrow("NEXT COMPOUND");ChoiceTabs(Compound.entries.map {it.name},p.next.name,{vm.updateStrategy(p.copy(next=Compound.valueOf(it)))})
        }}
        item {Panel {
            Text("Make the assumptions yours",fontSize=19.sp,fontWeight=FontWeight.SemiBold)
            Text("Rain scenario weight: ${p.rainPercent}%",fontSize=13.sp)
            Slider(p.rainPercent.toFloat(),{vm.updateStrategy(p.copy(rainPercent=it.roundToInt()))},valueRange=0f..100f,modifier=Modifier.semantics {contentDescription="Assumed rain percentage"})
            Text(String.format(Locale.US,"Pit loss: %.0f seconds",p.pitLossSeconds),fontSize=13.sp)
            Slider(p.pitLossSeconds.toFloat(),{vm.updateStrategy(p.copy(pitLossSeconds=it.roundToInt().toDouble()))},valueRange=12f..35f,modifier=Modifier.semantics {contentDescription="Assumed pit loss"})
            Text(String.format(Locale.US,"Tyre degradation multiplier: %.2f×",p.degradationFactor),fontSize=13.sp)
            Slider(p.degradationFactor.toFloat(),{vm.updateStrategy(p.copy(degradationFactor=(it*20).roundToInt()/20.0))},valueRange=.5f..1.5f,modifier=Modifier.semantics {contentDescription="Assumed degradation multiplier"})
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Hypothetical safety-car stop",Modifier.weight(1f),fontSize=13.sp);Switch(p.safetyCar,{vm.updateStrategy(p.copy(safetyCar=it))})}
        }}
        if(error!=null)item {Text(error,color=MaterialTheme.colorScheme.error,fontSize=13.sp)}
        if(result!=null)item {Panel {
            Eyebrow("MODEL DELTA / VERSUS BASELINE",Coral)
            Text(String.format(Locale.US,"%+.1f s",result.medianDeltaSeconds),fontSize=50.sp,fontWeight=FontWeight.Light,color=if(result.medianDeltaSeconds<=0)Mint else Coral)
            Text("Negative is faster in the model. This is not a finishing position or calibrated forecast.",fontSize=12.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            DeltaChart(result.lapDeltas)
            Text(String.format(Locale.US,"Scenario P10 %+.1fs  /  P90 %+.1fs",result.lowDeltaSeconds,result.highDeltaSeconds),fontSize=12.sp)
            InfoNote("80 seeded scenarios. Percentiles describe this model’s assumptions, not real-world confidence. Baseline: pit lap ${(p.laps*.46).toInt()}, ${if(p.start==Compound.Hard)"Medium"else"Hard"} next; same weather/wear draws. Safety-car loss = 60% of entered pit loss.")
        }}
        item {Panel {Eyebrow("THE MODEL, EXPLAINED");Text("Each lap = 90s + compound pace offset + tyre age × degradation × wear multiplier − 0.025s × lap. Add the pit loss on the stop lap. Rain scenarios add a simplified late-race penalty to dry tyres.",fontSize=12.sp,lineHeight=20.sp);Text("This generic exercise uses two different dry compounds. It does not model traffic, team energy maps, wet-tyre changes, circuit-specific stop rules, incidents or penalties.",fontSize=12.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        item {OutlinedButton(onClick={
            val link="apex://explore/strategy?pit=${p.pitLap}&start=${p.start.name}&next=${p.next.name}&rain=${p.rainPercent}&loss=${p.pitLossSeconds}&wear=${p.degradationFactor}&sc=${p.safetyCar}"
            ShareService.moment(context,"APEX Strategy Lab","LAP ${p.pitLap}","HYPOTHETICAL · ${p.start.name} → ${p.next.name} · ${p.rainPercent}% rain assumption",link,eventName="STRATEGY LAB",kind=MomentKind.Hypothetical)
        },enabled=error==null,modifier=Modifier.fillMaxWidth()){Text("Share this scenario")}}
    }
}

@Composable
private fun DeltaChart(values:List<Double>) {
    val color=Coral;val grid=MaterialTheme.colorScheme.outlineVariant
    val max=values.maxOfOrNull {abs(it)}?.coerceAtLeast(1.0)?:1.0
    Column {
        Text("Cumulative model delta (seconds)",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(115.dp).semantics {contentDescription="Cumulative simulated strategy delta. Scale minus ${max.roundToInt()} to plus ${max.roundToInt()} seconds; laps 1 to ${values.size}."}) {
            drawLine(grid,Offset(0f,size.height/2),Offset(size.width,size.height/2),1f)
            val path=Path()
            values.forEachIndexed {i,d->val x=i.toFloat()/(values.size-1).coerceAtLeast(1)*size.width;val y=size.height/2-(d/max*size.height*.44).toFloat();if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
            drawPath(path,color,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("LAP 1",fontSize=9.sp);Text("0 s baseline",fontSize=9.sp);Text("LAP ${values.size}",fontSize=9.sp)}
    }
}

@Composable
fun PitWallScreen(vm:F1ViewModel) {
    var question by rememberSaveable {mutableStateOf("")}
    var answer by remember {mutableStateOf<PitWallAnswer?>(null)}
    var connection by rememberSaveable {mutableStateOf(false)}
    var endpoint by rememberSaveable {mutableStateOf(vm.aiEndpoint)}
    var accessToken by remember {mutableStateOf(vm.aiToken)}
    var id by rememberSaveable {mutableStateOf(vm.selectedDriver.value)}
    fun ask(text:String) {
        if(text.isBlank())return
        vm.clearAi()
        val driver=vm.drivers.first {it.id==id};val health=vm.race.health.value
        answer=PitWallEngine.explain(text,PitWallContext(driver.code,vm.race.timing.value.find {it.driverId==id},vm.race.session.value.status,vm.race.session.value.demo,health.lastEventMs,health.lastArrivalMs,vm.strategyPlan))
        question=""
    }
    ApexLazyColumn(Modifier.testTag("pit-wall"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {ExploreHeading("APEX / AI PIT WALL","Understand the call.","Start with the evidence. Keep every assumption in sight.")}
        item {Badge("LOCAL EVIDENCE ENGINE",Coral);Spacer(Modifier.height(9.dp));InfoNote("The local analyst explains timing and the simulator without an LLM. Connect your own gateway below for an optional AI interpretation. Observed values always remain separate.")}
        item {DriverPicker(vm,id,{id=it;answer=null;vm.clearAi()})}
        item {ApexLazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(listOf("Why pit now?","What about rain?","What does a safety car change?")){q->OutlinedButton(onClick={ask(q)}){Text(q,fontSize=11.sp)}}}}
        item {
            OutlinedTextField(question,{question=it.take(400)},Modifier.fillMaxWidth(),label={Text("Ask your pit wall")},placeholder={Text("Why stop for fresher tyres?")},minLines=2,maxLines=4,shape=RoundedCornerShape(12.dp))
            Button(onClick={ask(question)},enabled=question.isNotBlank(),modifier=Modifier.fillMaxWidth().padding(top=9.dp)){Text("Explain with evidence")}
        }
        val a=answer
        if(a!=null) {
            item {Text(a.question,fontSize=23.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));Text(a.summary,fontSize=14.sp,lineHeight=23.sp)}
            items(a.evidence){e->Panel {Badge(if(e.kind==EvidenceKind.Observed&&a.source.startsWith("APEX"))"DEMO OBSERVATION"else e.kind.name.uppercase(),when(e.kind){EvidenceKind.Observed->Mint;EvidenceKind.Assumed->Amber;EvidenceKind.Estimated->Coral;EvidenceKind.Unavailable->MaterialTheme.colorScheme.onSurfaceVariant});Text(e.text,fontSize=13.sp,lineHeight=21.sp)}}
            item {SourceStamp(a.source,a.arrivalTimeMs?:System.currentTimeMillis(),simulated=a.source.startsWith("APEX"));a.eventTimeMs?.let {Text("Event time: ${UtcTime.format(it)}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            item {OutlinedButton(onClick={vm.askAi(a)},enabled=!vm.aiLoading&&vm.aiEndpoint.isNotBlank()&&vm.aiToken.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text(if(vm.aiLoading)"Asking your AI gateway…"else"Ask connected AI to interpret")}}
            if(vm.aiLoading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(vm.aiError!=null)item {InfoNote(vm.aiError.orEmpty())}
            val interpretation=vm.aiInterpretation
            if(interpretation!=null)item {Panel {Badge("AI INTERPRETATION · UNVERIFIED",Amber);Text(interpretation.analysis,fontSize=13.sp,lineHeight=22.sp);Text("${interpretation.model} · Evidence: ${interpretation.citations.joinToString()}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);InfoNote("Model-generated interpretation can be wrong. Citation IDs are validated against the supplied evidence; this does not independently verify every statement.")}}
        }
        item {TextButton(onClick={connection=!connection}){Text(if(connection)"Hide AI connection settings"else"Connect an AI backend")}}
        if(connection)item {Panel {
            Eyebrow("OPTIONAL HTTPS GATEWAY",Coral)
            OutlinedTextField(endpoint,{endpoint=it.take(300)},Modifier.fillMaxWidth(),label={Text("Gateway base URL")},placeholder={Text("https://your-gateway.example")},singleLine=true)
            OutlinedTextField(accessToken,{accessToken=it.take(512)},Modifier.fillMaxWidth(),label={Text("Gateway access token")},singleLine=true,visualTransformation=PasswordVisualTransformation())
            Button(onClick={vm.configureAi(endpoint,accessToken)},modifier=Modifier.fillMaxWidth()){Text("Save gateway settings")}
            InfoNote("Use your APEX gateway token, never a model provider key. The token is kept in memory and must be re-entered after a restart. A server and provider credentials are required. Nothing is sent until you tap Ask connected AI.")
        }}
        item {OutlinedButton(onClick={vm.openExperience("strategy")},Modifier.fillMaxWidth()){Text("Change assumptions in Strategy Simulator")}}
    }
}

@Composable
fun TimeMachineScreen(vm:F1ViewModel) {
    val clip=vm.replayClip
    var cursor by rememberSaveable(clip.title,vm.replayRequestId){mutableFloatStateOf(vm.replaySeekMs.coerceIn(0L,clip.durationMs).toFloat())}
    var playing by rememberSaveable(clip.title,vm.replayRequestId){mutableStateOf(false)}
    var speed by rememberSaveable {mutableStateOf("1×")}
    val markers=remember(clip,cursor){ReplayEngine.at(clip,cursor.toLong())}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_STOP)playing=false}
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(playing,clip,speed) {
        var last=System.nanoTime()
        while(playing) {delay(100);val now=System.nanoTime();val elapsed=(now-last)/1_000_000f;last=now;cursor=(cursor+elapsed*if(speed=="4×")4 else 1).coerceAtMost(clip.durationMs.toFloat());if(cursor>=clip.durationMs)playing=false}
    }
    val context=LocalContext.current
    ApexLazyColumn(Modifier.testTag("time-machine"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {ExploreHeading("APEX / RACE TIME MACHINE","The race. In your time.","Scrub recorded samples, inspect a moment, then let the clock run.")}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(clip.demo,{playing=false;vm.useDemoReplay()},label={Text("Exhibition")});FilterChip(!clip.demo,{playing=false;vm.loadArchive()},enabled=!vm.replayLoading,label={Text("Monaco 2024 archive")})}}
        item {Badge(if(clip.demo)"SIMULATED REPLAY"else"RECORDED COORDINATES",if(clip.demo)Amber else Mint);Text(clip.title,Modifier.padding(top=9.dp),fontSize=20.sp,fontWeight=FontWeight.SemiBold)}
        if(vm.replayLoading)item {LinearProgressIndicator(Modifier.fillMaxWidth());Text("Loading one recorded lap for LEC and PIA…",fontSize=12.sp)}
        if(vm.replayError!=null)item {Panel {Text("Archive unavailable",fontWeight=FontWeight.Bold);Text(vm.replayError.orEmpty(),fontSize=12.sp);OutlinedButton(onClick=vm::loadArchive,enabled=!vm.replayLoading){Text("Retry archive")}}}
        item {
            ReplayMap(clip,markers)
            Row(Modifier.fillMaxWidth().padding(top=10.dp),verticalAlignment=Alignment.CenterVertically){Text(String.format(Locale.US,"%02d:%02d",cursor.toInt()/60000,cursor.toInt()/1000%60),Modifier.weight(1f),fontSize=32.sp,fontWeight=FontWeight.Light);TextButton(onClick={speed=if(speed=="1×")"4×"else"1×"}){Text(speed)};Button(onClick={if(cursor>=clip.durationMs)cursor=0f;playing=!playing}){Text(if(playing)"Pause"else"Play")}}
            Slider(cursor,{cursor=it;playing=false},valueRange=0f..clip.durationMs.toFloat(),modifier=Modifier.semantics {contentDescription="Replay time in milliseconds"})
            Text("${UtcTime.format(UtcTime.parse(clip.startUtc)+cursor.toLong())}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(listOf("leclerc","piastri")){id->
            val m=markers.find {it.driverId==id};val d=vm.drivers.first {it.id==id}
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                DriverDoodle(d.code,d.color,Modifier.size(42.dp),reducedMotion=true)
                Column(Modifier.weight(1f).padding(start=12.dp)){Text(d.name,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text(if(m==null)"No sample within 1.5 seconds"else"Last recorded coordinate at ${m.timeMs/1000.0}s",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                Text(m?.speedKph?.let {"$it km/h"}?:"—",fontSize=17.sp,color=d.color)
            }
        }
        item {SourceStamp(clip.source,clip.fetchedAt,simulated=clip.demo);InfoNote(if(clip.demo)"Both the track and positions are simulated. Use the archive option to request a recorded lap from OpenF1."else"One actual 2024 Monaco lap (lap 10), not the full race or video. Markers use received x/y samples; samples over 1.5s old disappear. No car position is reconstructed from lap timing. Gaps over 1.5 seconds break the trace. Speeds are nearest earlier car samples, if available.")}
        item {OutlinedButton(onClick={ShareService.moment(context,clip.title,if(clip.demo)"DEMO REPLAY"else"RECORDED LAP",UtcTime.format(UtcTime.parse(clip.startUtc)+cursor.toLong()),"apex://explore/replay?mode=${if(clip.demo)"demo"else"recorded"}&t=${cursor.toLong()}",eventName=if(clip.demo)"MONACO EXHIBITION"else"MONACO 2024 / LAP 10",kind=if(clip.demo)MomentKind.Demo else MomentKind.Recorded)},Modifier.fillMaxWidth()){Text("Share this moment")}}
    }
}

@Composable
private fun ReplayMap(clip:ReplayClip,markers:List<ReplayPoint>) {
    val grid=MaterialTheme.colorScheme.outlineVariant;val text=MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.fillMaxWidth().height(270.dp).clip(PanelShape).background(MaterialTheme.colorScheme.surface).semantics {contentDescription="${if(clip.demo)"Simulated"else"Recorded"} circuit positions for Leclerc and Piastri"}) {
        fun screen(x:Float,y:Float)=Offset(size.width/2+x*minOf(size.width,size.height)*.82f,size.height/2+y*minOf(size.width,size.height)*.82f)
        for(i in 0..8)drawLine(grid.copy(alpha=.25f),Offset(0f,i*size.height/8),Offset(size.width,i*size.height/8),1f)
        val trace=Path()
        val lead=clip.points.filter {it.driverId=="leclerc"}
        if(clip.demo)clip.route.forEachIndexed {i,(x,y)->val p=screen(x,y);if(i==0)trace.moveTo(p.x,p.y)else trace.lineTo(p.x,p.y)}
        else lead.forEachIndexed {i,s->val p=screen(s.x,s.y);if(i==0||s.timeMs-lead[i-1].timeMs>1500)trace.moveTo(p.x,p.y)else trace.lineTo(p.x,p.y)}
        drawPath(trace,grid,style=Stroke(8.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round));drawPath(trace,text.copy(alpha=.6f),style=Stroke(1.dp.toPx()))
        markers.forEach {m->val p=screen(m.x,m.y);val color=if(m.driverId=="leclerc")Color(0xFFFF3D4F)else Color(0xFFFF9A43);drawCircle(text,8.dp.toPx(),p);drawCircle(color,5.dp.toPx(),p)}
    }
}

@Composable
fun RivalryScreen(vm:F1ViewModel) {
    var left by rememberSaveable {mutableStateOf("leclerc")};var right by rememberSaveable {mutableStateOf("piastri")}
    var mode by rememberSaveable {mutableStateOf("Exhibition")}
    val timing by vm.race.timing.collectAsStateWithLifecycle()
    val a=vm.drivers.first {it.id==left};val b=vm.drivers.first {it.id==right}
    LaunchedEffect(left,right,mode){if(mode=="2026 results")vm.loadRivalry(left,right,2026)}
    ApexLazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {ExploreHeading("APEX / RIVALRY LAB","Separated by fractions.","Compare the evidence. Keep the coverage in view.")}
        item {ChoiceTabs(listOf("Exhibition","2026 results"),mode,{mode=it})}
        item {Eyebrow("DRIVER A",Coral);DriverPicker(vm,left,{if(it!=right)left=it});Eyebrow("DRIVER B",Amber);DriverPicker(vm,right,{if(it!=left)right=it})}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround){Column(horizontalAlignment=Alignment.CenterHorizontally){DriverDoodle(a.code,a.color,Modifier.size(75.dp),vm.reducedMotion);Text(a.code,fontSize=23.sp,fontWeight=FontWeight.Bold,color=a.color)};Text("VS",Modifier.padding(top=35.dp),fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Column(horizontalAlignment=Alignment.CenterHorizontally){DriverDoodle(b.code,b.color,Modifier.size(75.dp),vm.reducedMotion);Text(b.code,fontSize=23.sp,fontWeight=FontWeight.Bold,color=b.color)}}}
        if(mode=="Exhibition") {
            item {Badge("SIMULATED CURRENT LAP",Amber)}
            val ra=timing.find {it.driverId==left};val rb=timing.find {it.driverId==right}
            if(ra!=null&&rb!=null) {
                item {ComparisonRow("Latest lap",formatLap(ra.lastLapMs),formatLap(rb.lastLapMs));Text(String.format(Locale.US,"A minus B: %+.3f s",(ra.lastLapMs-rb.lastLapMs)/1000.0),fontSize=24.sp,color=Coral)}
                items((0..2).toList()){i->ComparisonRow("Sector ${i+1}",ra.sectorsMs.getOrNull(i)?.let{String.format(Locale.US,"%.3fs",it/1000.0)}?:"Unavailable",rb.sectorsMs.getOrNull(i)?.let{String.format(Locale.US,"%.3fs",it/1000.0)}?:"Unavailable")}
            }else item {InfoNote("Waiting for exhibition timing samples.")}
            item {InfoNote("A same-clock sample-lap comparison. It does not establish qualifying pace, overtakes or driver racecraft.")}
        } else {
            val state=if(vm.rivalryKey=="$left/$right/2026")vm.rivalryResults else LoadState<RivalrySnapshot>(loading=true)
            item {DataLoading(state,{vm.loadRivalry(left,right,2026)})}
            val snapshot=state.snapshot
            if(snapshot!=null) {
                val r=snapshot.data
                item {SourceStamp(snapshot.source,snapshot.fetchedAt,snapshot.cached,snapshot.stale)}
                item {ComparisonRow("Races returned",r.left.size.toString(),r.right.size.toString());ComparisonRow("Race points",publishedPoints(r.left.sumOf {it.points}),publishedPoints(r.right.sumOf {it.points}));ComparisonRow("Podiums",r.left.count {it.finish<=3}.toString(),r.right.count {it.finish<=3}.toString());ComparisonRow("Grid → finish gains",r.left.sumOf {if(it.grid>0)it.grid-it.finish else 0}.toString(),r.right.sumOf {if(it.grid>0)it.grid-it.finish else 0}.toString())}
                item {Panel {Eyebrow("MATCHED Q1 PACE · ${r.qualifyingPairs} SESSIONS",Coral);Text(r.qualifyingDeltaMs?.let {String.format(Locale.US,"%+.3f s",it/1000)}?:"Unavailable",fontSize=32.sp,fontWeight=FontWeight.Light);InfoNote("Median A minus B for Q1 times in matching rounds. Includes changing track conditions; it is not normalized for fuel, tyres or weather. Missing times are excluded.")}}
                item {InfoNote("Grid-to-finish gains include pit strategy, retirements and penalties; they are not counted overtakes. Race points omit Sprint points. Racecraft and race-lap consistency are unavailable without suitable lap/incident data. No invented composite rating is shown.")}
            }
        }
    }
}

@Composable
private fun ComparisonRow(label:String,a:String,b:String) {
    Column(Modifier.padding(vertical=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Eyebrow(label.uppercase());Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(a,fontSize=23.sp,color=Coral,fontWeight=FontWeight.Medium);Text(b,fontSize=23.sp,color=Amber,fontWeight=FontWeight.Medium)};HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)}
}

@Composable
fun IntelligenceScreen(vm:F1ViewModel) {
    val events by vm.race.events.collectAsStateWithLifecycle()
    ApexLazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {ExploreHeading("APEX / RACE INTELLIGENCE","The story behind the signal.","Context for race-control messages. Source facts and possible implications stay separate.")}
        item {Badge("EXHIBITION EVENT STREAM",Amber)}
        if(events.isEmpty())item {EmptyState("Waiting for a race event","The exhibition feed adds events while the app is foregrounded.")}
        items(events.take(20),key={it.id}) {event->Panel {
            Badge(event.kind.name.uppercase(),if(event.kind==EventKind.RedFlag)F1Red else Coral)
            Text(event.title,fontSize=20.sp,fontWeight=FontWeight.Bold)
            Eyebrow("OBSERVED IN THE DEMO",Mint);Text(event.detail,fontSize=13.sp,lineHeight=21.sp)
            Eyebrow("POSSIBLE IMPLICATION",Amber)
            Text(when(event.kind){EventKind.RedFlag->"A suspension changes the race timeline. Restart procedure and tyre-change permissions require the applicable official race-control instructions.";EventKind.SafetyCar->"Neutralisation can compress gaps and alter pit loss. Whether a stop is advantageous depends on track position and actual timing.";EventKind.Pit->"A tyre change trades pit-lane time for a fresh stint. Compound, traffic and degradation determine the payoff.";EventKind.FastestLap->"One quick lap is a pace observation. It does not establish sustained stint pace or an automatic championship bonus.";else->"Check the next valid timing sample before concluding that the competitive order has changed."},fontSize=13.sp,lineHeight=21.sp)
            Text("Event time ${UtcTime.format(event.eventTimeMs)} · Lap ${event.lap}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={vm.openExperience("pitwall")}){Text("Explore the evidence →")}
        }}
    }
}

@Composable
fun CareerScreen(vm:F1ViewModel) {
    var id by rememberSaveable {mutableStateOf(vm.selectedDriver.value)}
    var season by rememberSaveable {mutableIntStateOf(2026)}
    val public=vm.archiveDriver
    val selected=vm.drivers.first {it.id==id}
    val providerId=public?.id?:id
    val name=public?.name?:selected.name
    val color=if(public!=null)F1Red else selected.color
    LaunchedEffect(providerId,season){vm.loadCareer(providerId,season)}
    val state=if(vm.careerKey=="$providerId/$season")vm.careerResults else LoadState<List<SeasonResult>>(loading=true)
    ApexLazyColumn(Modifier.testTag("career-universe"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {ExploreHeading("APEX / CAREER UNIVERSE","Every chapter counts.","Explore a driver’s actual race results, one season at a time.")}
        if(public==null)item {DriverPicker(vm,id,{id=it})}
        item {Row(verticalAlignment=Alignment.CenterVertically){DriverDoodle(public?.code?:selected.code,color,Modifier.size(86.dp),vm.reducedMotion);Column(Modifier.weight(1f).padding(start=12.dp)){Text(name,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("Season $season",fontSize=13.sp,color=Coral)}}}
        item {ApexLazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items((2026 downTo 2000).toList()){year->FilterChip(year==season,{season=year},label={Text(year.toString())})}}}
        item {DataLoading(state,{vm.loadCareer(providerId,season,true)})}
        val snapshot=state.snapshot
        if(snapshot!=null) {
            val results=snapshot.data
            item {SourceStamp(snapshot.source,snapshot.fetchedAt,snapshot.cached,snapshot.stale)}
            if(results.isEmpty())item {EmptyState("No published race results for $season","Choose another season. An empty provider response is not proof that a driver never raced.")}
            else {
                item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("RACES",results.size.toString());Metric("WINS",results.count {it.finish==1}.toString());Metric("PODIUMS",results.count {it.finish<=3}.toString())};Spacer(Modifier.height(10.dp));InfoNote("Totals cover the returned race results for $season, not the entire career. Sprint and qualifying results are outside this view.")}
                items(results,key={it.round}) {r->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.width(3.dp).height(45.dp).background(if(r.finish<=3)color else MaterialTheme.colorScheme.outlineVariant))
                    Column(Modifier.weight(1f).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Eyebrow("ROUND ${r.round} · ${r.date}");Text(r.race,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text("Grid ${if(r.grid==0)"pit lane/unavailable"else r.grid} · ${r.status}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    Text("P${r.finish}",fontSize=23.sp,fontWeight=FontWeight.Bold,color=if(r.finish<=3)color else MaterialTheme.colorScheme.onSurface)
                }}
            }
        }
        item {OutlinedButton(onClick={vm.openExperience("helmets")},Modifier.fillMaxWidth()){Text("Explore the fictional Helmet Vault")}}
    }
}
