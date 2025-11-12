package com.example.drivemap.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.drivemap.DriveMapApp
import com.example.drivemap.R
import com.example.drivemap.location.LocationTracker
import com.example.drivemap.ui.MainActivity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LocationRecorderService : LifecycleService() {
    private lateinit var tracker: LocationTracker

    override fun onCreate() {
        super.onCreate()
        tracker = LocationTracker(applicationContext)
        startForeground(NOTIFICATION_ID, buildNotification())
        observeLocations()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    private fun observeLocations() {
        val repository = (application as DriveMapApp).container.roadRepository
        lifecycleScope.launch {
            tracker.locations()
                .catch { /* ignore */ }
                .collect { sample ->
                    repository.recordLocation(sample)
                    val stationary = sample.speedMps?.let { it < STATIONARY_THRESHOLD_MPS } ?: true
                    repository.setStationary(stationary)
                }
        }
    }

    private fun buildNotification(): Notification {
        createChannel()
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.start_tracking))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "drive-map-tracking"
        private const val NOTIFICATION_ID = 42
        private const val STATIONARY_THRESHOLD_MPS = 0.4

        fun start(context: Context) {
            val intent = Intent(context, LocationRecorderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
