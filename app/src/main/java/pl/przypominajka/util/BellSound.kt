package pl.przypominajka.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.SoundPool
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

/**
 * Dźwięk dzwonu w aplikacji (przejścia między ekranami) i odsłuch dźwięków w Ustawieniach.
 * Gra jako dźwięk aplikacji – na głośności multimediów, nie jako powiadomienie.
 * Pliki: res/raw (syntezowane dzwony, zob. [BellTone]).
 */
class BellSound(private val context: Context, private val prefs: AppearancePrefs) {

    private val pool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(AppAudio)
            .build()
            .also { pool -> pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) } }
    }

    /** Zasób → identyfikator w SoundPool. */
    private val ids = ConcurrentHashMap<Int, Int>()
    private val ready = ConcurrentHashMap.newKeySet<Int>()
    private var lastPlayAt = 0L
    private var previewPlayer: MediaPlayer? = null
    private var previewRingtone: Ringtone? = null

    /** Wczytanie dźwięku przejść z wyprzedzeniem (w tle). */
    fun prepare() {
        load(prefs.transitionTone)
    }

    /** Uderzenie dzwonu przy przejściu – z uwzględnieniem ustawień i trybu cichego telefonu. */
    fun play() {
        if (!prefs.bellSoundEnabled) return
        if (prefs.quietWhenSilent && isPhoneSilenced()) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlayAt < MIN_INTERVAL_MS) return
        lastPlayAt = now
        val id = load(prefs.transitionTone) ?: return
        val volume = prefs.bellVolume
        if (id in ready && volume > 0f) pool.play(id, volume, volume, 1, 0, 1f)
    }

    /** Odsłuch w Ustawieniach (zawsze słychać – nawet przy wyciszonym dzwonie przejść). */
    fun preview(tone: BellTone, volume: Float = prefs.bellVolume) {
        stopPreview()
        val raw = tone.raw
        when {
            raw != null -> {
                previewPlayer = MediaPlayer.create(context, raw, AppAudio, 0)?.apply {
                    setVolume(volume.coerceAtLeast(0.15f), volume.coerceAtLeast(0.15f))
                    setOnCompletionListener { it.release(); if (previewPlayer === it) previewPlayer = null }
                    start()
                }
            }
            tone == BellTone.SYSTEM -> {
                previewRingtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                previewRingtone?.play()
            }
        }
    }

    fun stopPreview() {
        previewPlayer?.release()
        previewPlayer = null
        previewRingtone?.stop()
        previewRingtone = null
    }

    private fun load(tone: BellTone): Int? {
        val raw = tone.raw ?: return null
        return ids.getOrPut(raw) { pool.load(context, raw, 1) }
    }

    private fun isPhoneSilenced(): Boolean {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        return audio.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    private companion object {
        const val MIN_INTERVAL_MS = 250L

        /** Dźwięk aplikacji (strumień multimediów), a nie powiadomienie. */
        val AppAudio: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
