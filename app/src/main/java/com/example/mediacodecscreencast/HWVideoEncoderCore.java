package com.example.mediacodecscreencast;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@RequiresApi(LOLLIPOP)
public class HWVideoEncoderCore {
    private Context context;

    private static final int FRAME_RATE = 24;
    public static final int MAX_INPUT_SIZE = 0;
    private static final String VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;    // H.264 Advanced Video Coding
    private static final String AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int IFRAME_INTERVAL = 1;
    private final String mPath;

    private Surface mInputSurface;
    private MediaMuxer mMuxer;
    private MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;
    private MediaCodec.BufferInfo mVBufferInfo;
    private MediaCodec.BufferInfo mABufferInfo;
    private int mVTrackIndex;
    private int mATrackIndex;
    private boolean mMuxerStarted;
    private boolean mStreamEnded;
    private long mRecordStartedAt = 0;

    private RecordCallback mCallback;
    private Handler mMainHandler;
    private boolean mIsAudioEmpty;

    Callback cb = null;

    interface Callback {
        void onAudioFrameProceed();

        void onVideoFrameProceed();
    }

    private MediaCodec.Callback videoCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
            try {
                if (mMuxerStarted) {
                    if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        mStreamEnded = true;
                    }

                    if (index >= 0 && !mStreamEnded) {
                        mVBufferInfo = info;

                        ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(index);

                        if ((mVBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // The codec config data was pulled out and fed to the muxer when we got
                            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                            mVBufferInfo.size = 0;
                        }

                        if (mVBufferInfo.size != 0) {
                            // adjust the ByteBuffer values to match BufferInfo (not needed?)
                            encodedData.position(mVBufferInfo.offset);
                            encodedData.limit(mVBufferInfo.offset + mVBufferInfo.size);

                            mVBufferInfo.presentationTimeUs = System.nanoTime() / 1000;

                            mMuxer.writeSampleData(mVTrackIndex, encodedData, mVBufferInfo);
                        }

                        mVideoEncoder.releaseOutputBuffer(index, false);

                        if ((mVBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            mStreamEnded = true;
                        }
                    } else {
                        mVideoEncoder.releaseOutputBuffer(index, false);
                    }
                } else {
                    mVideoEncoder.releaseOutputBuffer(index, false);
                }

                if (cb != null) {
                    cb.onVideoFrameProceed();
                }
            } catch (Exception e) {
            }
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            // should happen before receiving buffers, and should only happen once
            if (mMuxerStarted) {
                throw new RuntimeException("format changed twice");
            }
            MediaFormat newFormat = mVideoEncoder.getOutputFormat();

            // now that we have the Magic Goodies, start the muxer
            mVTrackIndex = mMuxer.addTrack(newFormat);
            tryStartMuxer();
        }
    };


    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    public HWVideoEncoderCore(Context context, int width, int height, int bitRate, File outputFile)
            throws Exception {
        this.context = context;
        mMainHandler = new Handler(Looper.getMainLooper());

        mVBufferInfo = new MediaCodec.BufferInfo();
        mABufferInfo = new MediaCodec.BufferInfo();

        mVideoEncoder = createVideoEncoder(width, height, bitRate);

        mStreamEnded = false;

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert m
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        mPath = outputFile.toString();
        mMuxer = new MediaMuxer(mPath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        mVTrackIndex = -1;
        mATrackIndex = -1;
        mMuxerStarted = false;
    }

    private MediaCodec createVideoEncoder(int width, int height, int bitRate) {
        Size size = new Size(256, 256);
        MediaFormat videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, size.getWidth(), size.getHeight());
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        videoFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE, FRAME_RATE);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE);

        // Create a MediaCodec encoder, and configure it with our videoFormat.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        MediaCodec videoEncoder = null;
        try {
            videoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mInputSurface = videoEncoder.createInputSurface();
        videoEncoder.setCallback(videoCallback);
        videoEncoder.start();
        return videoEncoder;
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Releases encoder resources.
     */
    public void release() {
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }

        if (mMuxer != null) {
            try {
                if (mIsAudioEmpty) {
                    // avoid empty audio track. if the audio track is empty , muxer.stop will failed
                    byte[] bytes = new byte[2];
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    mABufferInfo.set(0, 2, System.nanoTime() / 1000, 0);
                    buffer.position(mABufferInfo.offset);
                    buffer.limit(mABufferInfo.offset + mABufferInfo.size);
                    mMuxer.writeSampleData(mATrackIndex, buffer, mABufferInfo);
                }
                mMuxer.stop();
                if (mCallback != null) {
                    mMainHandler.post(() -> {
                        File outFile = new File(mPath);
                        List<File> result = new ArrayList<>();
                        result.add(outFile);
//                        mCallback.onRecordSuccess(result, mCoverPath, System.currentTimeMillis() - mRecordStartedAt);
                    });
                }
            } catch (final IllegalStateException e) {
                if (mCallback != null) {
                    mMainHandler.post(() -> mCallback.onRecordFailed(e, System.currentTimeMillis() - mRecordStartedAt));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mMuxer.release();
            } catch (IllegalStateException ex) {
            }

            mMuxer = null;
        }
    }

    public void setRecordCallback(RecordCallback callback) {
        mCallback = callback;
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream) {
        if (endOfStream) {
            mVideoEncoder.signalEndOfInputStream();
            mStreamEnded = true;
        }
    }

    private ByteBuffer[] splitBuffer(ByteBuffer buffer, int size, int n) {
        ByteBuffer[] buffers = new ByteBuffer[n];
        int chunkSize = size / n;
        for (int i = 0; i < n; i++) {
            byte[] chunk = new byte[chunkSize];
            buffer.get(chunk, 0, chunkSize);
            buffers[i] = ByteBuffer.wrap(chunk);
        }
        return buffers;
    }

    private void tryStartMuxer() {
        if (mVTrackIndex != -1 && !mMuxerStarted) { // and muxer not started // then start the muxer
            mMuxer.start();
            mMuxerStarted = true;
            mRecordStartedAt = System.currentTimeMillis();
        }
    }

    private Handler createHandler(String name) {
        HandlerThread thread = new HandlerThread(name);
        thread.start();
        return new Handler(thread.getLooper());
    }
}
