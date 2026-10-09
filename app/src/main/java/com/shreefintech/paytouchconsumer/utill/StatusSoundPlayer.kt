package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * Single-instance sound player for in-app feedback sounds.
 * Usage: create one per Activity, call play() to fire a sound, release() in onPause/onDestroy.
 *
 * Uses STREAM_MUSIC (media volume) — sound respects the user's media volume setting.
 * Stops any previously playing sound before starting a new one.
 */
class StatusSoundPlayer {

    private var player: MediaPlayer? = null

    fun play(context: Context, @RawRes res: Int) {
        release()
        try {
            val afd = context.resources.openRawResourceFd(res) ?: return
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnCompletionListener { it.release(); player = null }
                setOnErrorListener { mp, _, _ -> mp.release(); player = null; true }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Utility.logError(e)
        }
    }

    fun release() {
        try { player?.stop() } catch (_: Exception) {}
        player?.release()
        player = null
    }
}
