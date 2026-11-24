package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ChannelMessageEntity
import com.example.ui.F1ViewModel
import com.example.ui.components.*

@Composable
fun CommunityScreen(vm: F1ViewModel) {
    val channel by vm.selectedChannel.collectAsStateWithLifecycle()
    val messages by vm.chatMessages.collectAsStateWithLifecycle()
    var draft by rememberSaveable(channel) { mutableStateOf("") }
    var reply by rememberSaveable(channel) { mutableStateOf<String?>(null) }
    var guidelines by remember { mutableStateOf(false) }
    val reactions = remember { mutableStateMapOf<String, Boolean>() }
    val fixtures = remember(channel) { listOf(
        ChannelMessageEntity(-1, channel, "Sofia", "Track position is everything here. Watching those pit windows closely.", 0, "#E49AA8"),
        ChannelMessageEntity(-2, channel, "Arjun", "The sector comparison makes the pace difference much easier to see.", 0, "#51D6A2"),
        ChannelMessageEntity(-3, channel, "Theo", "Who are you backing in your exhibition fantasy lineup?", 0, "#C89BFF")
    ) }
    ApexLazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.imePadding()) {
        item {
            Eyebrow("GOOD RACING. GREAT COMPANY.", Coral)
            Text("Find your paddock.", fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Text("A shared love for the next corner.", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        item {
            SectionTitle("In the grandstands", "Guidelines") { guidelines = true }
            InfoNote("Community preview · messages and reactions stay on this device. Sample fans are fictional; no one is connected online.")
        }
        item {
            ApexLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf("Race control lounge", "Tifosi corner", "Papaya grandstand")) { room ->
                    Column(Modifier.width(188.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Glyph("mic", color = Coral, size = 17.dp); Spacer(Modifier.width(8.dp)); Eyebrow("VOICE PREVIEW") }
                        Text(room, Modifier.height(48.dp), fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("S", "A", "T").forEachIndexed { index, initial ->
                                Box(Modifier.size(28.dp).background(listOf(Coral, Mint, Violet)[index].copy(alpha=.15f), CircleShape), contentAlignment = Alignment.Center) { Text(initial, fontSize = 10.sp, color = listOf(Coral, Mint, Violet)[index]) }
                            }
                        }
                        Text("3 sample participants", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        OutlinedButton(onClick = { vm.previewVoice(room) }, Modifier.fillMaxWidth()) { Text("Preview room", fontSize = 11.sp) }
                    }
                }
            }
        }
        if (vm.previewRoom != null) item {
            Panel {
                Badge("PREVIEW · NO AUDIO CONNECTION", Amber)
                Text(vm.previewRoom!!, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Glyph("muted", color = Amber)
                    Text("Microphone off · audio unavailable", fontSize = 12.sp)
                }
                InfoNote("This is a local room preview. No audio is connected, and your microphone is off.")
                OutlinedButton(onClick = vm::leaveVoice) { Text("Leave preview") }
            }
        }
        item {
            SectionTitle("The conversation")
            ApexLazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf("Race room", "Leclerc fans", "McLaren", "Strategy talk")) { room ->
                    FilterChip(channel == room, { vm.selectChannel(room) }, label = { Text(room, fontSize = 11.sp) })
                }
            }
        }
        items((fixtures + messages).filter { it.username !in vm.blockedUsers }, key = { "message-" + it.id }) { message ->
            var menu by remember { mutableStateOf(false) }
            val key = "$channel:${message.id}"
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(message.avatarColorHex)) }.getOrDefault(Coral)
                    Box(Modifier.size(30.dp).background(color.copy(alpha=.12f), CircleShape), contentAlignment = Alignment.Center) { Text(message.username.take(1), fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold) }
                    Text(message.username, Modifier.padding(start = 9.dp).weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Eyebrow(if (message.id < 0) "SAMPLE" else "LOCAL")
                    Box {
                        ActionIcon("more", "Message options for " + message.username, { menu = true })
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Reply") }, onClick = { reply = message.username; menu = false })
                            DropdownMenuItem(text = { Text("Report message") }, onClick = { vm.reportMessage(key); menu = false })
                            if (message.username != "You") DropdownMenuItem(text = { Text("Block " + message.username) }, onClick = { vm.blockUser(message.username); menu = false })
                        }
                    }
                }
                Text(message.message, Modifier.padding(start = 39.dp), fontSize = 13.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(start = 34.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { reactions[key] = reactions[key] != true }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(if (reactions[key] == true) "♥ 1" else "♡ React", fontSize = 10.sp, color = if (reactions[key] == true) Coral else MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = { reply = message.username }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Reply", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.4f), thickness = .5.dp)
            }
        }
        item {
            if (reply != null) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Replying to $reply", Modifier.weight(1f), color = Coral, fontSize = 11.sp)
                ActionIcon("close", "Cancel reply", { reply = null })
            }
            OutlinedTextField(draft, { draft = it.take(500) }, Modifier.fillMaxWidth(), placeholder = { Text("Add to the conversation…", fontSize = 13.sp) },
                supportingText = { Text("Local chat · ${draft.length}/500", fontSize = 10.sp) }, shape = RoundedCornerShape(12.dp), maxLines = 4,
                trailingIcon = {
                    IconButton(onClick = { vm.postChatMessage(if (reply != null) "@$reply $draft" else draft); draft = ""; reply = null }, enabled = draft.isNotBlank(), modifier = Modifier.semantics { contentDescription = "Send local message" }) { Glyph("send", color = if (draft.isNotBlank()) Coral else MaterialTheme.colorScheme.outline) }
                })
        }
    }
    if (guidelines) AlertDialog(onDismissRequest = { guidelines = false }, title = { Text("Keep it sporting.") },
        text = { Text("Respect other fans. No harassment, hate, impersonation or personal information. Report abuse and block unwanted interactions.\n\nThis is a local community preview. Reports are stored on this device; online moderation and abuse prevention require a community backend.", lineHeight = 23.sp) },
        confirmButton = { TextButton(onClick = { guidelines = false }) { Text("Understood") } })
}
