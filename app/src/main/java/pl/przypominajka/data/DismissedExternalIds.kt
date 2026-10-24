package pl.przypominajka.data

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Wpisy z Librusa, które użytkownik usunął ręcznie. Synchronizacja ich nie przywraca.
 */
class DismissedExternalIds(private val prefs: SharedPreferences) {

    fun all(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()

    fun add(externalId: String) {
        prefs.edit { putStringSet(KEY, all() + externalId) }
    }

    fun removeWithPrefix(prefix: String) {
        prefs.edit { putStringSet(KEY, all().filterNot { it.startsWith(prefix) }.toSet()) }
    }

    private companion object {
        const val KEY = "dismissed_external_ids"
    }
}
