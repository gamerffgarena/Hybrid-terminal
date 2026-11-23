package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ForegroundSessionStatus(
    val isRunning: Boolean = false,
    val uptimeSeconds: Long = 0L,
    val activeModeLabel: String = "Terminal (Low Power)",
    val wakeLockHeld: Boolean = false
)

object SessionServiceState {
    private val _status = MutableStateFlow(ForegroundSessionStatus())
    val status: StateFlow<ForegroundSessionStatus> = _status.asStateFlow()

    fun update(transform: (ForegroundSessionStatus) -> ForegroundSessionStatus) {
        _status.update(transform)
    }
}

class SessionForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundCompat()
                SessionServiceState.update {
                    it.copy(isRunning = false, uptimeSeconds = 0L, wakeLockHeld = false)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_MODE -> {
                val modeLabel = intent.getStringExtra(EXTRA_MODE_LABEL) ?: "Terminal (Low Power)"
                SessionServiceState.update { it.copy(activeModeLabel = modeLabel) }
                if (SessionServiceState.status.value.isRunning) {
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, buildNotification(modeLabel, SessionServiceState.status.value.uptimeSeconds))
                }
                return START_STICKY
            }
            else -> {
                val modeLabel = intent?.getStringExtra(EXTRA_MODE_LABEL)
                    ?: SessionServiceState.status.value.activeModeLabel
                startForegroundWithNotification(modeLabel)
                startUptimeTicker()
                return START_STICKY
            }
        }
    }

    private fun startForegroundWithNotification(modeLabel: String) {
        SessionServiceState.update {
            it.copy(
                isRunning = true,
                activeModeLabel = modeLabel,
                wakeLockHeld = true
            )
        }
        val notification = buildNotification(modeLabel, SessionServiceState.status.value.uptimeSeconds)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startUptimeTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                SessionServiceState.update { current ->
                    if (!current.isRunning) current
                    else current.copy(uptimeSeconds = current.uptimeSeconds + 1L)
                }
                val snapshot = SessionServiceState.status.value
                if (snapshot.isRunning && snapshot.uptimeSeconds % 15L == 0L) {
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(
                        NOTIFICATION_ID,
                        buildNotification(snapshot.activeModeLabel, snapshot.uptimeSeconds)
                    )
                }
            }
        }
    }

    private fun buildNotification(modeLabel: String, uptimeSec: Long): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, SessionForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mins = uptimeSec / 60
        val secs = uptimeSec % 60
        val uptimeFormatted = String.format("%02d:%02d", mins, secs)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_stat_terminal)
            .setContentTitle("TermDeck Session Active")
            .setContentText("$modeLabel • Uptime $uptimeFormatted • PTY/proot alive")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Session", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TermDeck Background Sessions",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps PTY shell and proot Linux desktop bridges alive in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        SessionServiceState.update {
            it.copy(isRunning = false, wakeLockHeld = false)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "termdeck_session_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_UPDATE_MODE = "com.example.service.ACTION_UPDATE_MODE"
        const val EXTRA_MODE_LABEL = "extra_mode_label"

        fun start(context: Context, modeLabel: String) {
            val intent = Intent(context, SessionForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE_LABEL, modeLabel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateMode(context: Context, modeLabel: String) {
            if (!SessionServiceState.status.value.isRunning) return
            val intent = Intent(context, SessionForegroundService::class.java).apply {
                action = ACTION_UPDATE_MODE
                putExtra(EXTRA_MODE_LABEL, modeLabel)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SessionForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
