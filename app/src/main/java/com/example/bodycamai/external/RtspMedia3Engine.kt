package com.example.bodycamai.external

import android.content.Context
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView

/**
 * Real RTSP playback using AndroidX Media3/ExoPlayer.
 * Health is promoted to STREAMING only after ExoPlayer reports a rendered first frame.
 */
class RtspMedia3Engine(
    context: Context,
    override val source: ExternalVideoSource,
    private val onRuntime: (ExternalStreamController.Runtime) -> Unit = {}
) : ExternalMediaEngine {
    private val appContext = context.applicationContext
    private val controller = ExternalStreamController(source)
    private val player = ExoPlayer.Builder(appContext).build()
    private val playerView = PlayerView(appContext).apply {
        useController = false
        player = this@RtspMedia3Engine.player
    }

    private var prepared = false

    init {
        player.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                onRuntime(controller.onFrame(System.currentTimeMillis()))
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val current = controller.onFrame(System.currentTimeMillis())
                onRuntime(current.copy(
                    source = current.source.copy(
                        width = videoSize.width.takeIf { it > 0 },
                        height = videoSize.height.takeIf { it > 0 },
                            )
                ))
            }

            override fun onPlayerError(error: PlaybackException) {
                onRuntime(controller.fail(error.message ?: error.errorCodeName))
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE) {
                    onRuntime(controller.heartbeat(System.currentTimeMillis()))
                }
            }
        })
    }

    override val view: View
        get() = playerView

    override fun prepare(): Result<Unit> = runCatching {
        require(source.kind == ExternalSourceKind.RTSP) { "Source is not RTSP" }
        val uri = source.uri?.takeIf { it.startsWith("rtsp://", true) }
            ?: throw IllegalArgumentException("Invalid RTSP URI")
        val mediaSource = RtspMediaSource.Factory()
            .createMediaSource(MediaItem.fromUri(uri))
        player.setMediaSource(mediaSource)
        player.prepare()
        prepared = true
        onRuntime(controller.start().getOrThrow())
    }

    override fun play(): Result<Unit> = runCatching {
        check(prepared) { "RTSP engine is not prepared" }
        player.play()
    }

    override fun pause() {
        player.pause()
        onRuntime(controller.heartbeat(System.currentTimeMillis()))
    }

    override fun stop() {
        player.stop()
        onRuntime(controller.stop())
        prepared = false
    }

    override fun release() {
        controller.stop()
        player.release()
    }
}
