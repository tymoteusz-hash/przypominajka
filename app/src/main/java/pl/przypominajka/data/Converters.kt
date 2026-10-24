package pl.przypominajka.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** Pojedyncza godzina jako minuty od północy. */
    @TypeConverter
    fun fromLocalTime(time: LocalTime): Int = time.hour * 60 + time.minute

    @TypeConverter
    fun toLocalTime(minutes: Int): LocalTime = LocalTime.of((minutes / 60).coerceIn(0, 23), minutes % 60)

    /** Godziny zapisujemy jako minuty od północy, np. "480,1230" = 08:00 i 20:30. */
    @TypeConverter
    fun fromTimes(times: List<LocalTime>): String =
        times.joinToString(",") { (it.hour * 60 + it.minute).toString() }

    @TypeConverter
    fun toTimes(value: String): List<LocalTime> =
        value.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .map { minutes -> LocalTime.of(minutes / 60, minutes % 60) }
}
