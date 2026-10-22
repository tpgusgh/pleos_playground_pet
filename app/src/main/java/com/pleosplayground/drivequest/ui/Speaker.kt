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
                val result = tts?.setLanguage(Locale.KOREAN)
                ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
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
