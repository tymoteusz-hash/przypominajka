package pl.przypominajka.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
        ),
    ],
    indices = [Index(value = ["folderId"])],
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** O czym przypomnieć (wymagane). */
    val title: String,
    /** Opcjonalny opis / notatka. */
    val note: String = "",
    val folderId: Long,
    /** Pierwszy dzień przypominania. */
    val startDate: LocalDate,
    /** Liczba dni przypominania; null = bez końca. */
    val durationDays: Int?,
    /** Godziny przypomnień w ciągu dnia (posortowane, bez duplikatów). */
    val times: List<LocalTime>,
    /** Przełącznik włącz/wyłącz z ekranu głównego. */
    val enabled: Boolean = true,
    /** Ustawiane automatycznie po ostatnim powiadomieniu ostatniego dnia. */
    val finished: Boolean = false,
    /** Czas (epoch millis) odłożonego powiadomienia ("Przypomnij za 10 min") lub null. */
    val snoozedUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Rodzaj wpisu szkolnego ([SchoolType.name]) – tylko dla folderu "Szkoła", inaczej null. */
    val schoolType: String? = null,
    /** Identyfikator wpisu z zewnętrznego źródła, np. "librus:12345"; null dla wpisanych ręcznie. */
    val externalId: String? = null,
    /** Dzień wydarzenia szkolnego (sprawdzian, kartkówka…); przypomnienia kończą się dzień wcześniej. */
    val eventDate: LocalDate? = null,
    /**
     * true – godzina powiadomienia liczona osobno dla każdego dnia: godzinę po ostatniej lekcji
     * (z planu lekcji, a gdy go brak – z ustawień). [times] służy wtedy tylko jako zapas.
     */
    @ColumnInfo(defaultValue = "0")
    val afterSchool: Boolean = false,
    /**
     * Dni tygodnia jako maska bitów (bit 0 = poniedziałek … bit 6 = niedziela).
     * 0 = codziennie.
     */
    @ColumnInfo(defaultValue = "0")
    val daysOfWeek: Int = 0,
)

/** Czy przypomnienie przychodzi w dany dzień tygodnia. */
fun Reminder.firesOn(date: LocalDate): Boolean = daysOfWeek.includes(date.dayOfWeek)

fun Int.includes(day: java.time.DayOfWeek): Boolean = this == 0 || (this and day.bit) != 0

val java.time.DayOfWeek.bit: Int get() = 1 shl (value - 1)

/** Prefiks [Reminder.externalId] dla wpisów pobranych z terminarza Librusa. */
const val LIBRUS_EXTERNAL_PREFIX = "librus:"

val Reminder.isFromLibrus: Boolean
    get() = externalId?.startsWith(LIBRUS_EXTERNAL_PREFIX) == true

/** Ostatni dzień przypominania lub null, gdy przypomnienie jest bez końca. */
val Reminder.endDate: LocalDate?
    get() = durationDays?.let { startDate.plusDays(it.toLong() - 1) }

fun Reminder.isFinishedOn(today: LocalDate): Boolean {
    val end = endDate
    return finished || (end != null && today.isAfter(end))
}
