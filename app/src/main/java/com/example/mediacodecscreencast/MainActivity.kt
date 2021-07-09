package com.example.mediacodecscreencast

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.recordButton).setOnClickListener {
            if (isPermissionsGranted()) {
                requestProjection()
            } else {
                ActivityCompat.requestPermissions(this, permissions, 1)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requestProjection()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            startRecordingService(resultCode, data)
        }
    }

    private fun requestProjection() {
        if (isPermissionsGranted()) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 2)
        }
    }

    private fun startRecordingService(resultCode: Int, resultData: Intent?) {
        val intent = Intent(this, RecordingService::class.java).apply {
            putExtra(RecordingService.EXTRA_PROJECTION_RESULT_CODE, resultCode)
            putExtra(RecordingService.EXTRA_PROJECTION_RESULT_DATA, resultData)
        }
        startForegroundService(intent)

    }

    private fun isPermissionsGranted(): Boolean {
        return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }
}