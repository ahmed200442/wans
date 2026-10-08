package com.aistudio.wanas

import android.content.Context
import android.media.AudioManager

class MicController(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setEnabled(enabled: Boolean) {
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        audio.isMicrophoneMute = !enabled
    }

    fun reset() {
        audio.isMicrophoneMute = false
        audio.mode = AudioManager.MODE_NORMAL
    }
}
