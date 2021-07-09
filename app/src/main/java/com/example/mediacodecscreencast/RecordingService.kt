package com.example.mediacodecscreencast

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RecordingService : Service() {
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }

    private lateinit var recorder: PartialScreenRecorder

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel(this)
        val notification = NotificationCompat.Builder(this, getString(R.string.app_name))
            .build()
        startForeground(123, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_PROJECTION_RESULT_DATA)
        val resultCode = intent?.getIntExtra(EXTRA_PROJECTION_RESULT_CODE, Activity.RESULT_OK)
        if (resultCode != null && resultData != null) {
            val projection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
            recorder = PartialScreenRecorder(this, projection).apply { start() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        recorder.stop()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(getString(R.string.app_name), name, importance).apply {
                    description = descriptionText
                }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_PROJECTION_RESULT_CODE = "extra_projection_result_code"
        const val EXTRA_PROJECTION_RESULT_DATA = "extra_projection_result_data"
    }
}