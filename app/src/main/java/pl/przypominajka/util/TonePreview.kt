package pl.przypominajka.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager

/**
 * Odsłuch dźwięku powiadomień w Ustawieniach. Gra jako dźwięk aplikacji (głośność multimediów),
 * więc słychać go nawet przy wyciszonych powiadomieniach.
 */
class TonePreview(private val context: Context) {

    private var player: MediaPlayer? = null
    private var ringtone: Ringtone? = null

    fun play(tone: BellTone, volume: Float = 0.8f) {
        stop()
        val raw = tone.raw
        when {
            raw != null -> {
                val level = volume.coerceIn(0.15f, 1f)
                player = MediaPlayer.create(context, raw, AppAudio, 0)?.apply {
                    setVolume(level, level)
                    setOnCompletionListener { finished ->
                        finished.release()
                        if (player === finished) player = null
                    }
                    start()
                }
            }
            tone == BellTone.SYSTEM -> {
                ringtone = RingtoneManager.getRingtone(
                    context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                )
                ringtone?.play()
            }
        }
    }

    fun stop() {
        player?.release()
        player = null
        ringtone?.stop()
        ringtone = null
    }

    private companion object {
        /** Dźwięk aplikacji (strumień multimediów), a nie powiadomienie. */
        val AppAudio: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
