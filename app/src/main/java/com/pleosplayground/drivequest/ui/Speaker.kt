package com.pleosplayground.drivequest.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

interface Narrator {
    fun speak(text: String)
    fun shutdown()
}

class Speaker(context: Context) : Narrator {
    private var ready = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN
                ready = true
            }
        }
    }

    override fun speak(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "drivequest")
    }

    override fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
