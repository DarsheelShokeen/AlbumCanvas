package com.example.albumcanvas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.example.albumcanvas.MainActivity
import com.example.albumcanvas.R
import com.example.albumcanvas.data.MainRepository
import com.example.albumcanvas.data.SongDetails
import com.example.albumcanvas.data.pref.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlbumCanvasWidget : AppWidgetProvider() {

    @Inject lateinit var repository: MainRepository
    @Inject lateinit var settingsDataStore: SettingsDataStore

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        widgetScope.launch {
            val song = repository.currentSong.value
            val settings = settingsDataStore.settingsFlow.first()
            
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, song, settings.isServiceEnabled)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_SERVICE) {
            widgetScope.launch {
                val settings = settingsDataStore.settingsFlow.first()
                val nextState = !settings.isServiceEnabled
                settingsDataStore.updateSettings { it.copy(isServiceEnabled = nextState) }
                
                // Force update all widgets
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, AlbumCanvasWidget::class.java))
                val song = repository.currentSong.value
                for (id in ids) {
                    updateAppWidget(context, manager, id, song, nextState)
                }
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        song: SongDetails?,
        isServiceEnabled: Boolean
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Title and Artist
        if (song != null) {
            views.setTextViewText(R.id.widget_song_title, song.title)
            views.setTextViewText(R.id.widget_artist, song.artist)
            if (song.artwork != null) {
                views.setImageViewBitmap(R.id.widget_album_art, song.artwork)
            } else {
                views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_menu_gallery)
            }
        } else {
            views.setTextViewText(R.id.widget_song_title, "No Song Playing")
            views.setTextViewText(R.id.widget_artist, "Start playing music")
            views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_menu_gallery)
        }

        // Status Text & Toggle Indicator
        if (isServiceEnabled) {
            views.setTextViewText(R.id.widget_status, "Status: Active")
            views.setImageViewResource(R.id.widget_toggle_button, android.R.drawable.button_onoff_indicator_on)
        } else {
            views.setTextViewText(R.id.widget_status, "Status: Disabled")
            views.setImageViewResource(R.id.widget_toggle_button, android.R.drawable.button_onoff_indicator_off)
        }

        // Click to open App
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_album_art, appPendingIntent)

        // Click to Toggle Service State
        val toggleIntent = Intent(context, AlbumCanvasWidget::class.java).apply {
            action = ACTION_TOGGLE_SERVICE
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_toggle_button, togglePendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_TOGGLE_SERVICE = "com.example.albumcanvas.widget.ACTION_TOGGLE_SERVICE"
    }
}
