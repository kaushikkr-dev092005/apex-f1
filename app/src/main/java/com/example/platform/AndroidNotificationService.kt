package com.example.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.notifications.NotificationService
import com.example.data.race.EventKind
import com.example.data.race.RaceEvent

class AndroidNotificationService(private val context: Context) : NotificationService {
    override val remotePushConfigured = false
    override fun notify(event: RaceEvent): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val critical = event.kind in setOf(EventKind.RedFlag, EventKind.SafetyCar)
        val channel = if (critical) "apex_race_control" else "apex_lap_updates"
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channel, if (critical) "Race control" else "Lap updates", if (critical) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("apex://race/event/" + Uri.encode(event.id))
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, event.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(com.example.R.drawable.ic_apex_notification)
            .setContentTitle("DEMO · " + event.title).setContentText(event.detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.detail))
            .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
            .setGroup("apex_demo_events").setPriority(if (critical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching { NotificationManagerCompat.from(context).notify(event.id.hashCode(), notification); true }.getOrDefault(false)
    }
}
