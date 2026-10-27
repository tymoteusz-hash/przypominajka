package pl.przypominajka.ui.look

/**
 * Wygląd aplikacji (od wersji 4.0) – wybierany przy pierwszym uruchomieniu, zmieniany w Ustawieniach.
 * Każdy wygląd ma własny układ ekranów (osobny pakiet), kolory, fonty, kształty i ikonę aplikacji.
 */
enum class Look(val key: String, val label: String, val description: String) {
    KARTKA(
        key = "kartka",
        label = "Kartka",
        description = "Jasno i minimalnie: biel, grafit i jeden niebieski akcent.",
    ),
    DZWON(
        key = "dzwon",
        label = "Dzwon",
        description = "Złoty dzwon w środku, działy dookoła. Granat nocy i złoto.",
    ),
    TABLICA(
        key = "tablica",
        label = "Tablica",
        description = "Kolorowe karteczki na tablicy i pasek zakładek na dole.",
    ),
    ;

    companion object {
        /** null = wygląd jeszcze nie wybrany (albo nieznany zapis). */
        fun fromKey(key: String?): Look? = entries.firstOrNull { it.key == key }
    }
}
