package pl.przypominajka.librus

import org.json.JSONArray
import org.json.JSONObject
import pl.przypominajka.data.SchoolType
import pl.przypominajka.data.TimetableLesson
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

/** Wpis z terminarza Librusa (endpoint "HomeWorks" – mimo nazwy to sprawdziany, kartkówki itp.). */
data class LibrusEvent(
    val id: Long,
    val date: LocalDate,
    val categoryName: String,
    val subjectName: String?,
    val content: String,
    val lessonNo: Int?,
    /** null – kategoria, której nie przypominamy (np. zadanie domowe, zebranie). */
    val type: SchoolType?,
) {
    companion object {

        fun parse(
            json: JSONObject,
            categories: Map<Long, String>,
            subjects: Map<Long, String>,
        ): LibrusEvent? {
            val id = json.optLong("Id", -1L).takeIf { it > 0 } ?: return null
            val date = parseDate(json.string("Date")) ?: return null
            val categoryName = json.optJSONObject("Category")?.optLong("Id", -1L)?.let { categories[it] }.orEmpty()
            val subjectName = json.optJSONObject("Subject")?.optLong("Id", -1L)?.let { subjects[it] }
            return LibrusEvent(
                id = id,
                date = date,
                categoryName = categoryName,
                subjectName = subjectName?.takeIf { it.isNotBlank() },
                content = json.string("Content").trim(),
                lessonNo = json.string("LessonNo").toIntOrNull(),
                type = classify(categoryName),
            )
        }

        /** Rozpoznanie po nazwie kategorii nadanej przez szkołę, np. "Sprawdzian", "kartkówka", "Wycieczka". */
        fun classify(categoryName: String): SchoolType? {
            val name = categoryName.lowercase()
            return when {
                "kartk" in name -> SchoolType.QUIZ
                TEST_WORDS.any { it in name } -> SchoolType.TEST
                TRIP_WORDS.any { it in name } -> SchoolType.TRIP
                else -> null
            }
        }

        private val TEST_WORDS = listOf("sprawdz", "test", "klasówk", "praca klasowa", "egzamin", "próbn", "probn")
        private val TRIP_WORDS = listOf("wycieczk", "wyjści", "wyjazd", "wyjscie")

        private fun parseDate(text: String): LocalDate? = try {
            LocalDate.parse(text.take(10))
        } catch (e: DateTimeParseException) {
            null
        }
    }
}

/**
 * Lekcje z endpointu "Timetables?weekStart=…":
 * {"Timetable": {"2026-10-12": [[lekcja], [], [lekcja]…], …}} – każdy element tablicy dnia to jedna
 * godzina lekcyjna (pusta, gdy okienko).
 */
fun parseTimetable(json: JSONObject, subjects: Map<Long, String>): List<TimetableLesson> {
    val timetable = json.optJSONObject("Timetable") ?: return emptyList()
    val result = mutableListOf<TimetableLesson>()
    val dates = timetable.keys()
    while (dates.hasNext()) {
        val key = dates.next()
        val date = try {
            LocalDate.parse(key.take(10))
        } catch (e: DateTimeParseException) {
            continue
        }
        val slots: JSONArray = timetable.optJSONArray(key) ?: continue
        for (i in 0 until slots.length()) {
            val lessons = slots.optJSONArray(i) ?: continue
            for (j in 0 until lessons.length()) {
                val lesson = lessons.optJSONObject(j) ?: continue
                val start = parseHour(lesson.string("HourFrom")) ?: continue
                val end = parseHour(lesson.string("HourTo")) ?: continue
                val subjectJson = lesson.optJSONObject("Subject")
                val subject = subjectJson?.string("Name")?.takeIf { it.isNotBlank() }
                    ?: subjectJson?.optLong("Id", -1L)?.let { subjects[it] }
                    ?: ""
                val teacher = lesson.optJSONObject("Teacher")
                    ?.let { "${it.string("FirstName")} ${it.string("LastName")}".trim() }
                    .orEmpty()
                val classroom = lesson.optJSONObject("Classroom")
                    ?.let { room -> room.string("Symbol").ifBlank { room.string("Name") } }
                    .orEmpty()
                result += TimetableLesson(
                    date = date,
                    lessonNo = lesson.string("LessonNo").toIntOrNull() ?: i,
                    subject = subject,
                    startTime = start,
                    endTime = end,
                    canceled = lesson.optBoolean("IsCanceled", false),
                    teacher = teacher,
                    classroom = classroom,
                    substitution = lesson.optBoolean("IsSubstitutionClass", false),
                )
            }
        }
    }
    return result
}

/** "08:45" albo "08:45:00" → LocalTime. */
private fun parseHour(text: String): LocalTime? {
    val parts = text.trim().split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return LocalTime.of(hour, minute)
}

/** optString zwraca "null" dla wartości JSON null – tu zamieniamy to na pusty tekst. */
internal fun JSONObject.string(name: String): String =
    if (isNull(name)) "" else optString(name, "")
