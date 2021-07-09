package com.example.mediacodecscreencast

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.util.Size
import android.view.Surface
import java.io.File
import java.io.IOException

class VideoEncoderCore(private val context: Context, private val outFile: File) {
    private val bitRate = 256 * 256
    private val frameRate = 30
    private val iFrameInternal = 1
    private val maxInputSize = 0
    private val videoMimeType = MediaFormat.MIMETYPE_VIDEO_AVC

    private var muxer: MediaMuxer? = null
    private var videoCodec: MediaCodec? = null

    var inputSurface: Surface? = null
        private set

    private val videoCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            Log.d("ggwp", "onInputBufferAvailable")
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.d("ggwp", "onOutputFormatChanged")
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            Log.d("ggwp", "onOutputBufferAvailable")
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            Log.d("ggwp", "onError ${e.message}")
        }
    }

    init {
        val size = Size(256, 256)
        val videoFormat = MediaFormat.createVideoFormat(videoMimeType, size.width, size.height)
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInternal)
        videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize)

        try {
            videoCodec = MediaCodec.createEncoderByType(videoMimeType).apply {
                configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
                setCallback(videoCallback)
                start()
            }

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}