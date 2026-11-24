package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.F1ViewModel
import com.example.ui.DriverDoodle
import com.example.ui.components.*

@Composable
fun ProfileScreen(vm: F1ViewModel, onDriver: (String) -> Unit, onNotifications: () -> Unit) {
    val appearance by vm.appearance.collectAsStateWithLifecycle()
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    ApexLazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(19.dp)) {
        item {
            Eyebrow("YOUR SEAT IN THE PADDOCK", Coral)
            Text("Make the race yours.", fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(51.dp).background(Coral.copy(alpha=.15f), CircleShape), contentAlignment = Alignment.Center) { BrandMark() }
                Column(Modifier.padding(start = 13.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Race fan", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("Local profile · no sign-in needed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge("PADDOCK PASS", Coral)
            }
        }
        item {
            Panel {
                SectionTitle("Race-night ready")
                Eyebrow("APPEARANCE")
                ChoiceTabs(listOf("Dark", "Light", "System"), appearance.replaceFirstChar { it.uppercase() }, { vm.setAppearance(it.lowercase()) })
                SettingToggle("Reduced motion", "Keep transitions immediate.", vm.reducedMotion, vm::updateReducedMotion)
                SettingToggle("Larger text", "Increase text size across the app.", vm.largeText, vm::updateLargeText)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onNotifications), verticalAlignment = Alignment.CenterVertically) {
                    Glyph("bell", color = Coral)
                    Column(Modifier.weight(1f).padding(start = 11.dp)) {
                        Text("Notifications & alerts", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Your drivers. Your key moments.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Glyph("arrow", size = 16.dp)
                }
            }
        }
        item {
            SectionTitle("Drivers & standings")
            OutlinedButton(onClick={vm.openExperience("standings")},Modifier.fillMaxWidth()){Text("Published 2026 championship")}
            InfoNote("2024-inspired exhibition statistics. These are sample standings, not verified current championship results.")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Find a driver", fontSize = 13.sp) }, leadingIcon = { Glyph("search", size = 18.dp) }, singleLine = true, shape = RoundedCornerShape(11.dp))
        }
        val filtered = vm.drivers.filter { (it.name + it.code + it.team).contains(query, true) }
        if (filtered.isEmpty()) item { EmptyState("No drivers found", "Search by name, code or constructor.") }
        items(filtered, key = { it.id }) { driver ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable { onDriver(driver.id) }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${driver.position}".padStart(2,'0'), Modifier.width(30.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.size(3.dp,28.dp).background(driver.color))
                DriverDoodle(driver.code,driver.color,Modifier.size(39.dp),vm.reducedMotion,onOpenProfile={vm.selectDriver(driver.id);vm.archiveDriver=null;vm.openExperience("career")})
                Column(Modifier.weight(1f).padding(start = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(driver.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(driver.team, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Text("${driver.points}", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                ActionIcon(if (driver.id in alerts.followedDrivers) "check" else "plus", (if (driver.id in alerts.followedDrivers) "Unfollow " else "Follow ") + driver.name, { vm.followDriver(driver.id) }, if (driver.id in alerts.followedDrivers) Coral else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Panel {
                Eyebrow("DEMO CONNECTION TOOLS")
                Text("Test the recovery flow without a real data feed.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::interruptFeed, Modifier.weight(1f)) { Text("Drop feed", fontSize = 11.sp) }
                    OutlinedButton(onClick = vm::reconnect, Modifier.weight(1f)) { Text("Reconnect", fontSize = 11.sp) }
                }
            }
        }
        item {
            Text(ApexBrand.name + "  /  0.3.0", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
            Text(ApexBrand.tagline, Modifier.padding(top = 5.dp), fontSize = 12.sp, color = Coral)
            Spacer(Modifier.height(10.dp))
            InfoNote("Independent fan companion. No official affiliation. Offline demo, local fantasy and local community; live data, remote push and voice infrastructure are not configured.")
        }
    }
}

@Composable
fun SettingToggle(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 10.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(value, onChange, modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
fun NotificationPreferencesScreen(vm: F1ViewModel) {
    val prefs by vm.alerts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        denied = !allowed; vm.updateLocalNotifications(allowed)
        if (!allowed) vm.message("Android denied notifications. You can enable them in app settings.")
    }
    val categories = listOf(
        Triple("race", "Session start", "Race, sprint and qualifying start."),
        Triple("red", "Red flags", "Session suspension and critical race control."),
        Triple("safety", "Safety car & track status", "Safety car, virtual safety car and flags."),
        Triple("fastest", "Fastest & personal-best laps", "Major timing milestones."),
        Triple("lap", "Followed driver lap updates", "Throttled to once a minute per driver."),
        Triple("position", "Pit stops & position changes", "Strategy and race order changes."),
        Triple("finish", "Race finish", "Final classification when provided."),
        Triple("news", "Followed team & driver news", "Requires an editorial and push provider."),
        Triple("fantasy", "Fantasy deadlines & scoring", "Requires a fantasy backend."),
        Triple("community", "Replies & mentions", "Requires a community backend.")
    )
    ApexLazyColumn(verticalArrangement = Arrangement.spacedBy(15.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Text("Only the moments that matter.", fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp)
            Spacer(Modifier.height(12.dp))
            InfoNote("Remote push is not configured. You can enable real Android local notifications for this demo while the app is foregrounded. Background delivery needs a push service.")
        }
        item {
            SettingToggle("Demo device notifications", "Explicitly opt in to local exhibition alerts.", vm.localNotifications) { enabled ->
                if (!enabled) vm.updateLocalNotifications(false)
                else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else {
                    val allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
                    denied = !allowed
                    vm.updateLocalNotifications(allowed)
                }
            }
            if (denied) TextButton(onClick = {
                val settingsIntent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName))
                runCatching { context.startActivity(settingsIntent) }.onFailure { vm.message("Open notifications from Android app settings.") }
            }) { Text("Open Android notification settings") }
        }
        item { Eyebrow("RACE PRIORITIES") }
        items(categories, key = { it.first }) { (id, title, subtitle) ->
            SettingToggle(title, subtitle, id in prefs.enabled) { vm.toggleCategory(id) }
        }
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionTitle("Your drivers")
            ApexLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(vm.drivers, key = { it.id }) { driver -> FilterChip(driver.id in prefs.followedDrivers, { vm.followDriver(driver.id) }, label = { Text(driver.code, fontSize = 11.sp) }) }
            }
            SectionTitle("Your constructors")
            ApexLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(vm.constructors, key = { it.id }) { constructor -> FilterChip(constructor.id in prefs.followedConstructors, { vm.followConstructor(constructor.id) }, label = { Text(constructor.id.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }) }
            }
        }
        item {
            Panel {
                SettingToggle("Quiet hours", "Silence all demo alerts during this period.", prefs.quietEnabled) { vm.updateAlertPreferences(prefs.copy(quietEnabled = it)) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HourPicker("From", prefs.quietStart, { vm.updateAlertPreferences(prefs.copy(quietStart = it)) }, Modifier.weight(1f))
                    HourPicker("Until", prefs.quietEnd, { vm.updateAlertPreferences(prefs.copy(quietEnd = it)) }, Modifier.weight(1f))
                }
                SettingToggle("Limit repetitive updates", "At least 60 seconds between routine alerts.", prefs.throttle) { vm.updateAlertPreferences(prefs.copy(throttle = it)) }
                Eyebrow("NON-CRITICAL DELIVERY")
                ChoiceTabs(listOf("Immediate", "Digest"), if (prefs.digest) "Digest" else "Immediate", { vm.updateAlertPreferences(prefs.copy(digest = it == "Digest")) })
                InfoNote("Digest mode keeps routine events in the in-app timeline. Scheduled remote digests are unavailable without a backend. Quiet hours use your device's local time.")
            }
        }
        item {
            OutlinedButton(onClick = { vm.testNotification() }, Modifier.fillMaxWidth()) { Text("Send local red-flag test") }
            InfoNote("The development test bypasses category and quiet-hour filters so you can check Android permission and notification deep links. It never impersonates a live race alert.")
        }
    }
}

@Composable
private fun HourPicker(label: String, hour: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text("$label  " + hour.toString().padStart(2, '0') + ":00", fontSize = 12.sp) }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 280.dp)) {
            (0..23).forEach { value -> DropdownMenuItem(text = { Text(value.toString().padStart(2, '0') + ":00") }, onClick = { onChange(value); expanded = false }) }
        }
    }
}
