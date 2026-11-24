package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.*

/** Original vector character; no logos, photographs, sprites or continuous idle renderer. */
@Composable
fun DriverDoodle(id: String, color: Color, modifier: Modifier = Modifier, reducedMotion: Boolean = false,
                 pose: String = "Idle", draggable: Boolean = false, trigger: Int = 0, onOpenProfile: () -> Unit = {}) {
    val jump=remember(id) { Animatable(0f) }; val gesture=remember(id) { Animatable(0f) }
    val scope=rememberCoroutineScope()
    var drag by remember(id) { mutableStateOf(Offset.Zero) }
    val personality=remember(id) { abs(id.hashCode()%5) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(id,pose,reducedMotion,trigger,lifecycle) {
        gesture.snapTo(0f)
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { if(!reducedMotion) {
            if(pose=="Race") gesture.animateTo(8f,tween(2400,easing=LinearEasing))
            else if(pose!="Idle") gesture.animateTo(1f,tween(650))
            else while(true) { delay(2600L+personality*180L);gesture.animateTo(1f,tween(180));gesture.animateTo(0f,tween(180)) }
        } }
    }
    fun hop() { if(!reducedMotion)scope.launch {jump.animateTo(-16f,tween(140));jump.animateTo(0f,spring(dampingRatio=.4f,stiffness=450f))} }
    Canvas(modifier.size(56.dp).graphicsLayer {alpha=if(pose=="Exit")1f-gesture.value else 1f}.semantics {
        contentDescription="$id Paddock Pal, $pose. Tap to jump; hold for profile."
        onClick("Make pal jump") {hop();true}
        onLongClick("Open driver profile") {onOpenProfile();true}
        customActions=listOf(CustomAccessibilityAction("Open driver profile") {onOpenProfile();true})
    }.pointerInput(id,reducedMotion) { detectTapGestures(onTap={hop()},onLongPress={onOpenProfile()}) }
        .then(if(draggable)Modifier.pointerInput(id) {detectDragGestures(onDragEnd={drag=Offset.Zero},onDragCancel={drag=Offset.Zero}) { change,delta->change.consume();drag=Offset((drag.x+delta.x).coerceIn(-60.dp.toPx(),60.dp.toPx()),(drag.y+delta.y).coerceIn(-40.dp.toPx(),40.dp.toPx())) }}else Modifier)) {
        val scale=minOf(size.width/100f,size.height/120f)
        val t=gesture.value
        val stride=if(pose=="Race")sin(t*PI*2).toFloat() else 0f
        val entry=if(pose=="Arrive")(1f-t)*90f else 0f
        withTransform({translate(size.width/2-50*scale+drag.x+entry*scale,size.height/2-60*scale+jump.value*scale+drag.y);scale(scale,scale,Offset.Zero)}) {
            drawOval(Color.Black.copy(alpha=.23f),Offset(23f,106f),Size(54f,8f))
            fun limb(x1:Float,y1:Float,x2:Float,y2:Float,c:Color=Color(0xFF24252C),width:Float=9f) {drawLine(c,Offset(x1,y1),Offset(x2,y2),width,StrokeCap.Round)}
            val salute=pose=="Adjust helmet" || pose=="Celebrate"
            limb(39f,88f,32f-stride*12f,106f-abs(stride)*7f)
            limb(60f,88f,67f+stride*12f,106f-abs(stride)*7f)
            limb(31f-stride*12f,107f-abs(stride)*7f,23f-stride*11f,107f-abs(stride)*7f,color,7f)
            limb(66f+stride*12f,107f-abs(stride)*7f,74f+stride*11f,107f-abs(stride)*7f,color,7f)
            drawRoundRect(Color(0xFF23242B),Offset(32f,59f),Size(36f,33f),androidx.compose.ui.geometry.CornerRadius(9f))
            limb(36f,67f,if(salute)19f else 20f+stride*12f,if(salute)42f else 85f+stride*8f)
            limb(64f,67f,if(pose=="Celebrate")81f else 79f-stride*12f,if(pose=="Celebrate")36f else 83f-stride*8f)
            limb(49f,64f,49f,84f,color,5f)
            val headY=if(pose=="Disappointed")6f*t else if(pose=="Celebrate")-5f*t else -2f*t
            withTransform({translate(0f,headY);rotate(if(pose=="Disappointed")8f else personality-2f,Offset(50f,35f))}) {
                drawRoundRect(color,Offset(17f,5f),Size(66f,61f),androidx.compose.ui.geometry.CornerRadius(26f))
                drawRoundRect(Color(0xFFE8DFD8),Offset(19f,14f),Size(62f,9f),androidx.compose.ui.geometry.CornerRadius(5f))
                drawRoundRect(Brush.linearGradient(listOf(Color(0xFF24454F),Color(0xFF080C13))),Offset(16f,25f),Size(68f,27f),androidx.compose.ui.geometry.CornerRadius(12f))
                drawLine(Color(0xFF82A6B0),Offset(25f,29f),Offset(68f,29f),1.5f,StrokeCap.Round)
                drawRoundRect(Color(0xFF252731),Offset(40f,56f),Size(21f,6f),androidx.compose.ui.geometry.CornerRadius(2f))
                if(personality%2==0)drawLine(Color.White.copy(alpha=.4f),Offset(33f,9f),Offset(34f,21f),4f)
            }
            if(pose=="Celebrate")for(i in 0..5){val x=10f+i*16;drawLine(if(i%2==0)color else Color(0xFFFFDA93),Offset(x,8f-i%2*7f),Offset(x+3f,3f-i%2*7f),3f)}
        }
    }
}
