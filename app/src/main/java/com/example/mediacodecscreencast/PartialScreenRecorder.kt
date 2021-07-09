package com.example.mediacodecscreencast

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.opengl.EGL14
import android.os.Environment
import android.view.Surface
import java.io.File

class PartialScreenRecorder(private val context: Context, private val projection: MediaProjection) {
    private var mediaRecorder: TextureMovieEncoder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val file = createOutputFile()

    var isRecording = false

    fun start() {
        startRecorder()
    }

    fun stop() {
        stopRecorder()
    }

    private fun startRecorder() {
        configureMediaProjection()

        mediaRecorder = createVideoRecorder().apply {
            callback = object : TextureMovieEncoder.Callback {
                override fun onSurfaceAvailable(surface: Surface) {
                    virtualDisplay?.surface = surface
                }
            }
            this.start(file)
        }
//        mediaRecorder?.setEncoderCallback(object : EncoderCallback {
//            override fun onStartRecord() {
//                this@PartialScreenRecorder.callback?.onRecordStarted()
//            }
//
//            override fun onInputSurfacePrepared(surface: Surface) {
//                virtualDisplay?.surface = surface
//            }
//
//            override fun onEncoderPrepared() {
//                configureMediaProjection(intent)
//                isRecording = true
//            }
//
//            override fun onError(e: Throwable) {
//                callback?.onRecordFailed(e, 0)
//            }
//        })
        attachRecorder(file)
    }

    private fun stopRecorder() {
        stopProjection()
        isRecording = false
    }

    private fun createVideoRecorder(): TextureMovieEncoder {
        return TextureMovieEncoder(context)
    }


    private fun configureMediaProjection() {
        virtualDisplay = projection.createVirtualDisplay(
            context.getString(R.string.app_name),
            1,
            1,
            1,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            null,
            null,
            null
        )
    }

    @Synchronized
    fun attachRecorder(file: File) {
//        val eglContext = EGL14.eglGetCurrentContext()
//
//        val firstFrame = recordingSession.frames.firstOrNull()
//        val cropRect = getCropRect(firstFrame)
//
//        mediaRecorder?.apply {
//            startRecording()
//        }
    }

    @Synchronized
    fun detachRecorder() {
//        mediaRecorder?.stopRecording()
//        virtualDisplay?.surface = null
    }

    private fun stopProjection() {
//        detachRecorder()
//        virtualDisplay?.release()
//        projection?.stop()
//
//        virtualDisplay = null
//        projection = null
//        mediaRecorder = null
    }

    private fun createOutputFile(): File {
        val dir = File(Environment.getExternalStorageDirectory(), "mediacodecscreencast")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "output.mp4")
    }
}