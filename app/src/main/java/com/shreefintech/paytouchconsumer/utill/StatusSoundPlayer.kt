package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * Single-instance sound player for in-app feedback sounds.
 * Usage: create one per Activity, call play() to fire a sound, release() in onPause/onDestroy.
 *
 * Uses STREAM_MUSIC (media volume) — sound respects the user's media volume setting.
 * Silent when the phone is in silent or vibrate ringer mode.
 * Stops any previously playing sound before starting a new one.
 */
class StatusSoundPlayer {

    private var player: MediaPlayer? = null

    fun play(context: Context, @RawRes res: Int) {
        release()
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        try {
            context.resources.openRawResourceFd(res)?.use { afd ->
                // Assign before the calls that can throw so release() in catch frees it
                val mp = MediaPlayer()
                player = mp
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                mp.setOnCompletionListener { it.release(); player = null }
                mp.setOnErrorListener { p, _, _ -> p.release(); player = null; true }
                mp.prepare()
                mp.start()
            }
        } catch (e: Exception) {
            Utility.logError(e)
            release()
        }
    }

    fun release() {
        try {
            player?.stop()
        } catch (e: IllegalStateException) {
            // stop() before prepare/start is expected — not reported
            e.printStackTrace()
        }
        player?.release()
        player = null
    }
}
