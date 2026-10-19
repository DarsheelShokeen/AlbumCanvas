package com.example.albumcanvas.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.albumcanvas.AlbumCanvasApp
import com.example.albumcanvas.MainActivity

object NotificationHelper {

    fun createNotification(
        context: Context,
        appName: String,
        songTitle: String,
        isPlaying: Boolean
    ): Notification {
        val clickIntent = Intent(context, MainActivity::class.java)
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Pending Intent for Refresh Action
        // We can capture this in MediaSessionObserverService or create a BroadcastReceiver
        val refreshIntent = Intent("com.example.albumcanvas.REFRESH_WALLPAPER")
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            refreshIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, AlbumCanvasApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery) // Fallback icon
            .setContentTitle(appName)
            .setContentText(songTitle)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(clickPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(songTitle))
            .addAction(
                android.R.drawable.stat_notify_sync,
                "Refresh",
                refreshPendingIntent
            )


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }
}
