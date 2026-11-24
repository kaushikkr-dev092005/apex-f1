package com.example.ui.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.explore.*
import com.example.ui.components.*
import kotlin.math.*

data class MeshFace(val vertices: List<Vec3>, val color: Color, val part: String = "shell")

object MeshFactory {
    private fun box(center: Vec3, w: Float, h: Float, d: Float, color: Color, part: String): List<MeshFace> {
        val v = listOf(Vec3(-w,-h,-d),Vec3(w,-h,-d),Vec3(w,h,-d),Vec3(-w,h,-d),Vec3(-w,-h,d),Vec3(w,-h,d),Vec3(w,h,d),Vec3(-w,h,d)).map { Vec3(it.x/2,it.y/2,it.z/2) + center }
        return listOf(listOf(0,1,2,3),listOf(5,4,7,6),listOf(4,0,3,7),listOf(1,5,6,2),listOf(4,5,1,0),listOf(3,2,6,7)).map { MeshFace(it.map(v::get),color,part) }
    }
    fun helmet(color: Color, season: Int, visorOpen: Boolean, exploded: Boolean): List<MeshFace> {
        val faces = mutableListOf<MeshFace>()
        val shellLift = if (exploded) -.22f else 0f
        val stripe = if (season == 2024) Color(0xFFF3EDE8) else if (season == 2025) Color(0xFFE4B278) else Color(0xFFACE3D8)
        fun surface(theta: Double, phi: Double, radius: Float = 1f) = Vec3((sin(theta)*cos(phi)*.65*radius).toFloat(),(sin(phi)*.77*radius).toFloat()+shellLift,(cos(theta)*cos(phi)*.67*radius).toFloat())
        for (ring in 0 until 12) for (slice in 0 until 24) {
            val p0 = -PI/2 + ring * PI * .88/12; val p1 = -PI/2 + (ring+1)*PI*.88/12
            val t0 = slice*PI*2/24; val t1 = (slice+1)*PI*2/24
            if(abs((t0+t1)/2-PI)<1.0 && (p0+p1)/2 in -.2.. .28)continue
            val stripeBand = slice % 12 in (if(season==2026) 4..5 else 0..1)
            val shade = if (stripeBand) stripe else color
            faces.add(MeshFace(listOf(surface(t0,p0),surface(t1,p0),surface(t1,p1),surface(t0,p1)),shade,"shell"))
        }
        for (i in 0 until 16) {
            val t0=PI-.95+i*1.9/16; val t1=PI-.95+(i+1)*1.9/16
            val explode = if (exploded) Vec3(0f,.05f,-.32f) else Vec3(0f,0f,0f)
            val vertices = listOf(surface(t0,-.2,1.025f),surface(t1,-.2,1.025f),surface(t1,.24,1.025f),surface(t0,.24,1.025f)).map {
                if(!visorOpen)it+explode else {
                    val py=it.y+.16f-shellLift;val pz=it.z+.59f
                    Vec3(it.x,py*cos(-2f)-pz*sin(-2f)-.16f+shellLift,py*sin(-2f)+pz*cos(-2f)-.59f)+explode
                }
            }
            faces.add(MeshFace(vertices,if(i<8) Color(0xFF183A42) else Color(0xFF0B151D),"visor"))
            faces.add(MeshFace(listOf(surface(t0,-.24,.94f),surface(t1,-.24,.94f),surface(t1,.3,.94f),surface(t0,.3,.94f)),Color(0xFF202028),"liner"))
        }
        for(i in 0 until 16) {
            val a=i*PI*2/16;val b=(i+1)*PI*2/16;val y=.51f+if(exploded).25f else 0f
            fun p(t:Double,r:Float,h:Float)=Vec3(sin(t).toFloat()*r,h,cos(t).toFloat()*r)
            faces.add(MeshFace(listOf(p(a,.48f,y),p(b,.48f,y),p(b,.3f,y),p(a,.3f,y)),Color(0xFF25252D),"liner"))
            faces.add(MeshFace(listOf(p(a,.3f,y),p(b,.3f,y),p(b,.3f,y+.1f),p(a,.3f,y+.1f)),Color(0xFF15151C),"liner"))
        }
        return faces
    }
    fun car(color: Color, exploded: Boolean): List<MeshFace> {
        val faces=mutableListOf<MeshFace>()
        fun add(center: Vec3,w:Float,h:Float,d:Float,c:Color,part:String) {
            val shift=if(exploded) when(part){"Front wing"->Vec3(0f,-.12f,-.25f);"Rear wing"->Vec3(0f,-.28f,.18f);"Power unit"->Vec3(0f,-.4f,0f);"Floor"->Vec3(0f,.17f,0f);else->Vec3(0f,0f,0f)} else Vec3(0f,0f,0f)
            faces += box(center+shift,w,h,d,c,part)
        }
        add(Vec3(0f,.25f,0f),.9f,.06f,2.4f,Color(0xFF24242A),"Floor")
        val sections=listOf(Triple(-1.33f,.075f,.18f),Triple(-.65f,.12f,.06f),Triple(-.1f,.27f,-.11f),Triple(.55f,.30f,-.08f),Triple(1f,.16f,.08f))
        fun section(s:Triple<Float,Float,Float>)=listOf(Vec3(-s.second,s.third,s.first),Vec3(s.second,s.third,s.first),Vec3(s.second,.25f,s.first),Vec3(-s.second,.25f,s.first))
        sections.zipWithNext().forEach {(a,b)->val aa=section(a);val bb=section(b);for(i in 0..3)faces.add(MeshFace(listOf(aa[i],bb[i],bb[(i+1)%4],aa[(i+1)%4]),color,"Chassis"))}
        add(Vec3(-.36f,.06f,.32f),.28f,.21f,1.1f,color,"Sidepods")
        add(Vec3(.36f,.06f,.32f),.28f,.21f,1.1f,color,"Sidepods")
        add(Vec3(0f,.12f,-.96f),.18f,.13f,.85f,color,"Nose")
        add(Vec3(0f,.16f,-1.35f),1.34f,.06f,.27f,color,"Front wing")
        add(Vec3(-.65f,.10f,-1.35f),.04f,.16f,.3f,color,"Front wing")
        add(Vec3(.65f,.10f,-1.35f),.04f,.16f,.3f,color,"Front wing")
        add(Vec3(0f,-.22f,1.12f),1.18f,.10f,.30f,color,"Rear wing")
        add(Vec3(-.52f,.0f,1.12f),.04f,.36f,.30f,color,"Rear wing")
        add(Vec3(.52f,.0f,1.12f),.04f,.36f,.30f,color,"Rear wing")
        add(Vec3(0f,-.20f,.40f),.28f,.28f,.58f,color,"Power unit")
        add(Vec3(0f,-.105f,-.20f),.29f,.04f,.42f,Color(0xFF08090D),"Cockpit")
        add(Vec3(-.17f,-.22f,-.22f),.04f,.05f,.46f,Color(0xFFBEC2C8),"Halo")
        add(Vec3(.17f,-.22f,-.22f),.04f,.05f,.46f,Color(0xFFBEC2C8),"Halo")
        add(Vec3(0f,-.22f,-.44f),.35f,.05f,.04f,Color(0xFFBEC2C8),"Halo")
        for(x in listOf(-.65f,.65f)) for(z in listOf(-.86f,.85f)) {
            // Cylindrical wheels, axle along x.
            for(i in 0 until 18) {
                val a=i*PI*2/18; val b=(i+1)*PI*2/18
                val v=listOf(Vec3(x-.15f,.14f+cos(a).toFloat()*.29f,z+sin(a).toFloat()*.29f),Vec3(x+.15f,.14f+cos(a).toFloat()*.29f,z+sin(a).toFloat()*.29f),Vec3(x+.15f,.14f+cos(b).toFloat()*.29f,z+sin(b).toFloat()*.29f),Vec3(x-.15f,.14f+cos(b).toFloat()*.29f,z+sin(b).toFloat()*.29f))
                faces.add(MeshFace(v,Color(0xFF17191D),"Tyres"))
                for(side in listOf(-.151f,.151f)) faces.add(MeshFace(listOf(Vec3(x+side,.14f,z),v[if(side<0)0 else 1].copy(x=x+side),v[if(side<0)3 else 2].copy(x=x+side)),if(i%3==0)Color(0xFF353841)else Color(0xFF22242A),"Tyres"))
            }
        }
        return faces
    }
}

@Composable
fun MeshViewer(id: String, faces: List<MeshFace>, modifier: Modifier = Modifier, selectedPart: String? = null, stage: Boolean = false, reducedMotion: Boolean = false, lighting: String = "Studio", airflow: Boolean = false) {
    var yaw by rememberSaveable(id) { mutableFloatStateOf(-.45f) }
    var pitch by rememberSaveable(id) { mutableFloatStateOf(.22f) }
    var zoom by rememberSaveable(id) { mutableFloatStateOf(1f) }
    val edge = Color.White.copy(alpha=.035f)
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if(lighting=="Warm")Color(0xFF1C1112)else Color(0xFF08090D))) {
        Canvas(Modifier.fillMaxSize().pointerInput(id) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed=false)
                var rotating=false
                do {
                    val e=awaitPointerEvent(); val pan=e.calculatePan(); val fingers=e.changes.count { it.pressed }
                    if(fingers>=2 || rotating || abs(pan.x)>abs(pan.y)*1.3f) {
                        rotating=true; yaw+=pan.x*.009f
                        if(fingers>=2) { pitch=(pitch+pan.y*.006f).coerceIn(-.7f,1.2f); zoom=(zoom*e.calculateZoom()).coerceIn(.65f,2f) }
                        if(pan!=Offset.Zero || fingers>=2)e.changes.forEach { it.consume() }
                    }
                } while(e.changes.any { it.pressed })
            }
        }.semantics { contentDescription="Rotatable 3D $id. Drag horizontally to rotate; pinch to zoom and tilt. Use the buttons for accessible rotation." }) {
            val scale=minOf(size.width*.42f,size.height*.42f)*zoom*(if(stage).74f else 1f)
            val center=Offset(size.width/2,size.height*.49f)
            fun screen(v:Vec3):Offset { val p=Mini3D.project(v,yaw,pitch); return center+Offset(p.x*scale,p.y*scale) }
            if(stage) {
                val floor=Path().apply { listOf(Vec3(-.38f,.46f,-2.1f),Vec3(.38f,.46f,-2.1f),Vec3(.38f,.46f,2.1f),Vec3(-.38f,.46f,2.1f)).forEachIndexed { i,v->val p=screen(v);if(i==0)moveTo(p.x,p.y)else lineTo(p.x,p.y) };close() }
                drawPath(floor,Brush.verticalGradient(listOf(Color(0xFF420E20),Color(0xFFEC254A),Color(0xFF3B0D1D))))
            } else {
                drawOval(Brush.radialGradient(listOf(Color(0xFF6D3C50).copy(alpha=.3f),Color.Transparent)),topLeft=Offset(size.width*.15f,size.height*.7f),size=androidx.compose.ui.geometry.Size(size.width*.7f,size.height*.2f))
            }
            val projected=faces.map { face -> face to face.vertices.map { Mini3D.project(it,yaw,pitch) } }.sortedByDescending { (_,v)->v.sumOf { it.depth.toDouble() }/v.size }
            projected.forEach { (face,verts) ->
                val path=Path().apply { verts.forEachIndexed { i,v ->val p=center+Offset(v.x*scale,v.y*scale);if(i==0)moveTo(p.x,p.y)else lineTo(p.x,p.y) };close() }
                val a=face.vertices[0];val b=face.vertices[1];val c=face.vertices[2]
                val ux=b.x-a.x;val uy=b.y-a.y;val uz=b.z-a.z;val vx=c.x-a.x;val vy=c.y-a.y;val vz=c.z-a.z
                val nx=uy*vz-uz*vy;val ny=uz*vx-ux*vz;val nz=ux*vy-uy*vx
                val length=sqrt(nx*nx+ny*ny+nz*nz).coerceAtLeast(.001f)
                val light=((if(lighting=="Night").5f else .80f)+(-nx*.12f-ny*.28f-nz*.22f)/length).coerceIn(.38f,1.12f)
                val base=face.color
                val color=if(selectedPart!=null&&selectedPart!=face.part) Color(.12f,.12f,.15f) else Color((base.red*light).coerceAtMost(1f),(base.green*light).coerceAtMost(1f),(base.blue*light).coerceAtMost(1f))
                drawPath(path,color);drawPath(path,edge,style=Stroke(.6f))
            }
            if(airflow)for(i in -2..2) {
                val flow=Path().apply {for(n in 0..28){val z=-1.7f+n*.12f;val p=screen(Vec3(i*.24f,-.5f-abs(i)*.04f-sin(n*.12f)*.12f,z));if(n==0)moveTo(p.x,p.y)else lineTo(p.x,p.y)}}
                drawPath(flow,Color(0xFF69C9DC).copy(alpha=.6f),style=Stroke(1.4.dp.toPx()))
            }
        }
        Row(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
            TextButton(onClick={yaw-=.35f}, modifier=Modifier.semantics {contentDescription="Rotate model left"}) { Text("↶",color=Color.White,fontSize=21.sp) }
            TextButton(onClick={yaw+=.35f}, modifier=Modifier.semantics {contentDescription="Rotate model right"}) { Text("↷",color=Color.White,fontSize=21.sp) }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
            ActionIcon("minus","Zoom out 3D model",{zoom=(zoom*.8f).coerceAtLeast(.65f)},Color.White)
            ActionIcon("plus","Zoom in 3D model",{zoom=(zoom*1.2f).coerceAtMost(2f)},Color.White)
            ActionIcon("fit","Reset 3D model",{yaw=-.45f;pitch=.22f;zoom=1f},Color.White)
        }
    }
}

@Composable
fun CircuitRelief(circuit: Circuit2027, modifier: Modifier = Modifier, interactive: Boolean = false, raised: Boolean = true, reducedMotion: Boolean = false) {
    var yaw by rememberSaveable(circuit.id) { mutableFloatStateOf(-.2f) }
    var tilt by rememberSaveable(circuit.id) { mutableFloatStateOf(.82f) }
    var zoom by rememberSaveable(circuit.id) { mutableFloatStateOf(1f) }
    val reveal=remember(circuit.id) { androidx.compose.animation.core.Animatable(if(interactive)0f else 1f) }
    LaunchedEffect(circuit.id,reducedMotion) { if(reducedMotion)reveal.snapTo(1f)else reveal.animateTo(1f,androidx.compose.animation.core.tween(320)) }
    val red=Color(0xFFFF3D4F)
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF111116))) {
        Canvas(Modifier.fillMaxSize().then(if(interactive) Modifier.pointerInput(circuit.id) {
            detectTransformGestures { _,pan,scale,_ -> yaw+=pan.x*.008f;tilt=(tilt+pan.y*.004f).coerceIn(.25f,1.5f);zoom=(zoom*scale).coerceIn(.65f,2.2f) }
        }else Modifier).semantics { contentDescription="3D circuit outline: ${circuit.country}. Raised height is decorative, not surveyed elevation." }) {
            val scale=minOf(size.width*.78f,size.height*.92f)*zoom
            val lift=if(raised).10f*reveal.value else 0f
            fun p(point:Pair<Float,Float>,height:Float):Offset {val v=Mini3D.project(Vec3(point.first,height,point.second),yaw,tilt);return Offset(size.width/2+v.x*scale,size.height*.49f+v.y*scale)}
            fun path(height:Float)=Path().apply { circuit.points.forEachIndexed { i,v->val xy=p(v,height);if(i==0)moveTo(xy.x,xy.y)else lineTo(xy.x,xy.y) } }
            drawPath(path(.09f),Color(0xFF040407),style=Stroke(if(interactive)12.dp.toPx() else 4.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            if(raised) {
                for(i in circuit.points.indices step if(interactive)1 else 4) {
                    drawLine(Color(0xFF8D1B31),p(circuit.points[i],.02f),p(circuit.points[i],-lift),if(interactive)4.dp.toPx()else 1.5.dp.toPx())
                }
                drawPath(path(.02f),Color(0xFF87182B),style=Stroke(if(interactive)4.dp.toPx()else 1.5.dp.toPx(),join=StrokeJoin.Round))
            }
            drawPath(path(-lift),red,style=Stroke(if(interactive)3.dp.toPx()else 1.5.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            if(interactive)drawPath(path(-lift-.007f),Color(0xFFFFADAF).copy(alpha=.6f),style=Stroke(.7.dp.toPx(),join=StrokeJoin.Round))
        }
        if(interactive) Row(Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
            ActionIcon("minus","Zoom out circuit",{zoom=(zoom/1.2f).coerceAtLeast(.65f)},Color.White)
            ActionIcon("plus","Zoom in circuit",{zoom=(zoom*1.2f).coerceAtMost(2.2f)},Color.White)
            ActionIcon("fit","Reset circuit sculpture",{yaw=-.2f;tilt=.82f;zoom=1f},Color.White)
        }
    }
}
