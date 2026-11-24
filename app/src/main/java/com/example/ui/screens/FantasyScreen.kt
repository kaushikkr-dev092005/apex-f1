package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.fantasy.FantasyEngine
import com.example.platform.ShareService
import com.example.ui.F1ViewModel
import com.example.ui.DriverDoodle
import com.example.ui.components.*
import java.util.Locale

@Composable
fun FantasyScreen(vm: F1ViewModel) {
    val team by vm.fantasyTeam.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var showScores by rememberSaveable { mutableStateOf(false) }
    var leagueInput by rememberSaveable { mutableStateOf("") }
    var inviteInput by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    LaunchedEffect(team) { if (team != null) editing = false }
    ApexLazyColumn(modifier = Modifier.testTag("fantasy-feed"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Eyebrow("APEX FANTASY / EXHIBITION", Coral)
            Text("Back your instinct.", fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Text("Your grid. Your call. Your bragging rights.", Modifier.padding(top = 6.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF3C202C), Color(0xFF21151E)))).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Eyebrow("MONACO EXHIBITION", Color(0xFFE49AA8))
                    Text("Build your race weekend", Modifier.padding(top = 5.dp), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Badge("OPEN DEMO", Color(0xFFE49AA8))
            }
        }
        if (team == null || editing) {
            item {
                SectionTitle(if (team == null) "Create your team" else "Edit your team", if (team != null) "Cancel" else null) { vm.restoreDraft(); editing = false }
                OutlinedTextField(vm.draftTeamName, { vm.draftTeamName = it.take(30) }, Modifier.fillMaxWidth(), label = { Text("Team name") }, singleLine = true, shape = RoundedCornerShape(11.dp))
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Metric("REMAINING CREDITS", String.format(Locale.US, "%.1fM", vm.draftBudgetRemaining), color = if (vm.draftBudgetRemaining >= 0) Mint else MaterialTheme.colorScheme.error)
                    Column(horizontalAlignment = Alignment.End) {
                        Eyebrow("YOUR LINEUP")
                        Text("${vm.draftDrivers.size}/2 drivers · ${if (vm.draftConstructor != null) 1 else 0}/1 team", Modifier.padding(top = 7.dp), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { (vm.draftBudgetUsed / 100).toFloat().coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = if (vm.draftBudgetRemaining >= 0) Coral else MaterialTheme.colorScheme.error, trackColor = MaterialTheme.colorScheme.outlineVariant)
            }
            item { Eyebrow("01 / PICK TWO DRIVERS · TAP C TO CHOOSE CAPTAIN") }
            items(vm.drivers, key = { "pick-" + it.id }) { driver ->
                val selected = driver.id in vm.draftDrivers
                Row(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (selected) driver.color.copy(alpha=.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.4f), RoundedCornerShape(12.dp))
                    .clickable { vm.toggleDraftDriver(driver.id) }.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    DriverDoodle(driver.code,driver.color,Modifier.size(38.dp),vm.reducedMotion,onOpenProfile={vm.selectDriver(driver.id);vm.archiveDriver=null;vm.openExperience("career")})
                    Column(Modifier.weight(1f).padding(start = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(driver.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(driver.team, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(String.format(Locale.US, "%.1fM", driver.cost), fontSize = 12.sp)
                    if (selected) TextButton(onClick = { vm.chooseDraftCaptain(driver.id) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(44.dp)) {
                        Text(if (vm.draftCaptain == driver.id) "C ×2" else "C", color = if (vm.draftCaptain == driver.id) Coral else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    } else Spacer(Modifier.width(12.dp))
                }
            }
            item { Eyebrow("02 / PICK ONE CONSTRUCTOR") }
            items(vm.constructors, key = { "team-" + it.id }) { constructor ->
                Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (vm.draftConstructor == constructor.id) constructor.color else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { vm.selectDraftConstructor(constructor.id) }.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(3.dp, 20.dp).background(constructor.color, RoundedCornerShape(2.dp)))
                    Text(constructor.name.replace("Formula 1 Team", "").replace("F1 Team", "").replace("Oracle ", "").replace("Mercedes-AMG PETRONAS", "Mercedes").take(26), Modifier.weight(1f).padding(start = 11.dp), fontSize = 12.sp)
                    Text(String.format(Locale.US, "%.1fM", constructor.cost), fontSize = 12.sp)
                    if (vm.draftConstructor == constructor.id) { Spacer(Modifier.width(8.dp)); Glyph("check", size = 16.dp, color = Mint) }
                }
            }
            item {
                Button(onClick = { vm.saveFantasyTeam() }, enabled = !vm.savingTeam, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(11.dp)) {
                    Text(if (vm.savingTeam) "Saving…" else "Save exhibition lineup")
                }
                Spacer(Modifier.height(12.dp))
                InfoNote("2 drivers + 1 constructor · 100M budget · captain earns 2× driver points. Exhibition entry stays open; no real race deadline applies.")
            }
        } else {
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(team!!.teamName, Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Badge("${team!!.totalPoints} PTS", Mint)
                    }
                    Eyebrow("MONACO SAMPLE RESULT / ROSTER SAVED ON DEVICE")
                    listOf(team!!.driver1Id, team!!.driver2Id).forEach { id ->
                        val driver = vm.drivers.first { it.id == id }
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(29.dp).background(driver.color))
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(driver.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(driver.team, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (id == vm.captain) Badge("C ×2", Coral)
                            Text("${FantasyEngine.score(id, id == vm.captain)}", Modifier.padding(start = 12.dp), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(vm.constructors.first { it.id == team!!.constructorId }.name, fontSize = 12.sp)
                    Text("Constructor: ${FantasyEngine.constructorScore(team!!.constructorId, vm.drivers)} pts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(onClick = { vm.restoreDraft(); editing = true }, Modifier.weight(1f)) { Text("Edit lineup", fontSize = 12.sp) }
                        Button(onClick = { showScores = !showScores }, Modifier.weight(1f)) { Text(if (showScores) "Hide scoring" else "How I scored", fontSize = 12.sp) }
                    }
                }
            }
            if (showScores) item {
                Panel {
                    SectionTitle("Every point, explained")
                    listOf(team!!.driver1Id, team!!.driver2Id).forEach { id ->
                        val result = FantasyEngine.sampleResults.first { it.driverId == id }
                        Text(vm.drivers.first { it.id == id }.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        FantasyEngine.breakdown(result, id == vm.captain).forEach { line ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(line.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+${line.points}", fontSize = 12.sp, color = Mint)
                            }
                        }
                    }
                    InfoNote("APEX rules: finish 25/18/15/12/10/8/6/4/2/1, +2 per position gained, +3 fastest lap. Captain doubles driver score. Constructor sums its listed drivers without multipliers. These are fixed sample results.")
                }
            }
            item {
                SectionTitle("The global grid")
                Eyebrow("EXHIBITION LEADERBOARD · SAMPLE RIVALS")
                val leaderboard = (listOf("Late Brakers" to 138, "Box Box Club" to 121, "Apex Hunters" to 93) + (team!!.teamName to team!!.totalPoints)).sortedByDescending { it.second }
                leaderboard.forEachIndexed { i, (name, points) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("0${i+1}", Modifier.width(35.dp), color = Coral, fontSize = 13.sp)
                        Text(name, Modifier.weight(1f), fontSize = 13.sp, fontWeight = if (name == team!!.teamName) FontWeight.Bold else FontWeight.Normal)
                        Text("$points", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Panel {
                    SectionTitle("Your private paddock")
                    if (vm.leagueCode.isNotEmpty()) {
                        Text(vm.leagueName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(vm.leagueCode, color = Coral, fontSize = 22.sp, letterSpacing = 3.sp)
                        Text("1 local member · your team leads the preview", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { if (!ShareService.text(context, "APEX exhibition invite", "Join my APEX league preview: ${vm.leagueCode}\napex://fantasy/league/${vm.leagueCode}\nLocal demo invite; online membership is not configured.")) vm.message("Sharing is unavailable.") }) { Text("Share invite") }
                    }
                    OutlinedTextField(leagueInput, { leagueInput = it.take(30) }, Modifier.fillMaxWidth(), label = { Text("New league name") }, singleLine = true)
                    Button(onClick = { vm.createLeague(leagueInput) }) { Text("Create local league") }
                    OutlinedTextField(inviteInput, { inviteInput = it.take(10) }, Modifier.fillMaxWidth(), label = { Text("Invite code · APX-MONACO") }, singleLine = true)
                    OutlinedButton(onClick = { vm.joinLeague(inviteInput) }) { Text("Join league preview") }
                    InfoNote("This league is stored on this device. Online competition and shared membership are not available in this demo.")
                }
            }
            item {
                Eyebrow("TRANSFER HISTORY")
                Text(vm.transferHistory.ifBlank { "No roster changes yet." }, Modifier.padding(top = 8.dp), fontSize = 12.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { if (!ShareService.moment(context, team!!.teamName, "${team!!.totalPoints} PTS", "APEX Fantasy · fixed sample result", "apex://fantasy/team")) vm.message("Sharing is unavailable.") }) { Text("Share fantasy result →") }
            }
        }
        item { InfoNote("Independent APEX exhibition game. No official fantasy affiliation, prizes, real-time scoring or online league integration is implied.") }
    }
}
