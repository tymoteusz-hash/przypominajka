package pl.przypominajka.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Kolor folderu w formacie ARGB. */
    val colorArgb: Int,
) {
    companion object {
        /**
         * Systemowy folder "Inne" (tworzony przy pierwszym uruchomieniu).
         * Nie można go usunąć – trafiają do niego przypomnienia z usuwanych folderów.
         */
        const val OTHER_FOLDER_ID = 4L

        /** Domyślny folder "Szkoła" – tylko w nim pojawia się wybór rodzaju wpisu. */
        const val SCHOOL_FOLDER_ID = 1L
    }
}

val Folder.isSystem: Boolean
    get() = id == Folder.OTHER_FOLDER_ID

/**
 * Paleta kolorów do wyboru dla folderów – przygaszone, ziemiste barwy o podobnej jasności
 * (pasują do złotego dzwonu i do siebie nawzajem; żadna nie "krzyczy").
 */
object FolderColors {
    val BLUE = 0xFF4E7397.toInt()
    val GREEN = 0xFF5E8B5A.toInt()
    val ORANGE = 0xFFC98A2B.toInt()
    val RED = 0xFFB5523A.toInt()
    val PURPLE = 0xFF87598A.toInt()
    val CYAN = 0xFF3F8A86.toInt()
    val YELLOW = 0xFFC9A227.toInt()
    val BROWN = 0xFF7A5A45.toInt()
    val PINK = 0xFFB5566B.toInt()
    val INDIGO = 0xFF4A5C8C.toInt()
    val LIME = 0xFF8A9A45.toInt()
    val GREY = 0xFF7D7568.toInt()

    val palette: List<Int> = listOf(
        BLUE, GREEN, ORANGE, RED, PURPLE, CYAN, YELLOW, BROWN, PINK, INDIGO, LIME, GREY,
    )

    /** Dawne, jaskrawe kolory (do wersji 3.0) → nowe odpowiedniki – używane w migracji bazy. */
    val legacyToNew: Map<Int, Int> = mapOf(
        0xFF1E88E5.toInt() to BLUE,
        0xFF43A047.toInt() to GREEN,
        0xFFFB8C00.toInt() to ORANGE,
        0xFFE53935.toInt() to RED,
        0xFF8E24AA.toInt() to PURPLE,
        0xFF00ACC1.toInt() to CYAN,
        0xFFFDD835.toInt() to YELLOW,
        0xFF6D4C41.toInt() to BROWN,
        0xFFD81B60.toInt() to PINK,
        0xFF3949AB.toInt() to INDIGO,
        0xFF7CB342.toInt() to LIME,
        0xFF546E7A.toInt() to GREY,
    )
}
