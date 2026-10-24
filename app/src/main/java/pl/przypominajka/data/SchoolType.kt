package pl.przypominajka.data

/** Rodzaj wpisu w folderze "Szkoła". W bazie zapisywana jest nazwa stałej ([name]). */
enum class SchoolType(
    val label: String,
    /** Etykieta pola tytułu po wybraniu tego rodzaju. */
    val titleLabel: String,
    /** Etykieta daty wydarzenia w formularzu. */
    val dateLabel: String,
    /** Domyślnie: ile dni przed terminem zacząć przypominać. */
    val defaultLeadDays: Int,
) {
    HOMEWORK("Zadanie domowe", "Co jest zadane? *", "Na kiedy?", 1),
    QUIZ("Kartkówka", "Z czego kartkówka? *", "Dzień kartkówki", 2),
    TEST("Sprawdzian", "Z czego sprawdzian? *", "Dzień sprawdzianu", 3),
    TRIP("Wyjście klasowe", "Dokąd wyjście? *", "Dzień wyjścia", 1),
    ;

    /** Do kartkówki i sprawdzianu można przygotować quiz. */
    val supportsQuiz: Boolean get() = this == QUIZ || this == TEST

    companion object {
        fun fromName(name: String?): SchoolType? = entries.firstOrNull { it.name == name }
    }
}
