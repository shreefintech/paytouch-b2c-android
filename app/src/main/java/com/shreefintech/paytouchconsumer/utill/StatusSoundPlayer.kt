package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.annotation.RawRes

class StatusSoundPlayer {
    private var player: MediaPlayer? = null

    fun play(context: Context, @RawRes res: Int) {
        release()
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        try {
            player = MediaPlayer.create(context, res)?.apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            Utility.logError(e)
            release()
        }
    }

    fun release() {
        player?.release()
        player = null
    }
}
