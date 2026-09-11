package com.core.voidapp.data.guardian

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/**
 * Guardian's voice output. Android's TextToSpeech engine does not expose
 * a "make this deeper" knob beyond pitch scaling on whatever voices the
 * device/engine actually ships — there is no guarantee any given phone
 * has a genuinely deep male voice installed. This picks the best
 * available candidate (a non-network, high-quality, male-leaning voice)
 * and pushes pitch/rate toward Guardian's serious, deliberate delivery;
 * where the device can't offer a deep voice, personality is preserved
 * through wording and pacing instead (see GuardianDialogue), per rule #28.
 */
object GuardianVoice {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var onOrbStateChange: ((GuardianOrbState) -> Unit)? = null

    fun setOrbStateListener(listener: (GuardianOrbState) -> Unit) {
        onOrbStateChange = listener
    }

    fun init(context: Context, onReady: (() -> Unit)? = null) {
        if (tts != null) {
            if (ready) onReady?.invoke()
            return
        }
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                applyVoicePreferences()
                onReady?.invoke()
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onOrbStateChange?.invoke(GuardianOrbState.SPEAKING)
            }
            override fun onDone(utteranceId: String?) {
                onOrbStateChange?.invoke(GuardianOrbState.IDLE)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onOrbStateChange?.invoke(GuardianOrbState.ERROR)
            }
        })
    }

    fun applyVoicePreferences() {
        val engine = tts ?: return
        val settings = GuardianRepository.settings()
        engine.language = Locale.getDefault()
        pickDeepestVoice(engine, settings.voiceName)?.let { engine.voice = it }
        engine.setSpeechRate(settings.speechRate)
        engine.setPitch(settings.pitch)
    }

    /**
     * Prefers: the user's explicitly chosen voice, then a non-network
     * (works offline, lower latency), higher-quality, male-leaning voice,
     * then simply the highest-quality voice available at all.
     */
    fun pickDeepestVoice(engine: TextToSpeech, preferredName: String?): Voice? {
        val voices = engine.voices ?: return null
        if (preferredName != null) {
            voices.find { it.name == preferredName }?.let { return it }
        }
        val offlineVoices = voices.filterNot { it.isNetworkConnectionRequired }
        val maleLeaning = offlineVoices.filter {
            it.name.contains("male", ignoreCase = true) && !it.name.contains("female", ignoreCase = true)
        }
        return (maleLeaning.ifEmpty { offlineVoices }.ifEmpty { voices.toList() })
            .maxByOrNull { it.quality }
    }

    fun availableVoices(context: Context): List<Voice> {
        init(context)
        return tts?.voices?.toList().orEmpty()
    }

    fun speak(context: Context, text: String) {
        init(context)
        val engine = tts ?: return
        val settings = GuardianRepository.settings()
        engine.setSpeechRate(settings.speechRate)
        engine.setPitch(settings.pitch)
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.volume)
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "guardian_${System.currentTimeMillis()}")
    }

    /** Plays a short sample line so the user can preview a voice/rate/pitch combination from Settings before committing to it. */
    fun testVoice(context: Context) {
        speak(context, "This is Guardian. This is how I will sound during your study sessions.")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
