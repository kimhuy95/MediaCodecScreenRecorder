package com.example.mediacodecscreencast

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.util.Log
import android.view.Surface
import com.example.mediacodecscreencast.gles.EglCore
import com.example.mediacodecscreencast.gles.MainFrameRect
import com.example.mediacodecscreencast.gles.Texture2dProgram
import com.example.mediacodecscreencast.gles.WindowSurface
import java.io.File

class TextureMovieEncoder(private val context: Context) {
    private var surface: Surface? = null
    private var surfaceTexture: SurfaceTexture? = null

    var callback: Callback? = null

    fun start(outFile: File) {
        val videoEncoder = VideoEncoderCore(context, outFile)

        val eglContext = EGL14.eglGetCurrentContext()
        val eglCore = EglCore(eglContext, EglCore.FLAG_RECORDABLE)
        val inputWindowSurface = WindowSurface(eglCore, videoEncoder.inputSurface, true)
        inputWindowSurface.makeCurrent()

        val fullScreen = MainFrameRect(Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
        val textureId = fullScreen.createTextureObject()
        surfaceTexture = SurfaceTexture(textureId).apply {
            setOnFrameAvailableListener {
                Log.d("ggwp", "onFrameAvailable ${System.currentTimeMillis()}")
                surfaceTexture?.updateTexImage()
            }
        }

        surface = Surface(surfaceTexture).apply { callback?.onSurfaceAvailable(this) }
    }

    fun stop() {
        surfaceTexture?.release()
        surface?.release()
    }

    interface Callback {
        fun onSurfaceAvailable(surface: Surface)
    }
}