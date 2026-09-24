package com.example.data.model

enum class ReadingThemeMode(val titleAr: String, val titleEn: String) {
    LIGHT("فاتح", "Light (Pure White)"),
    DARK("داكن", "Dark (Pure Black)"),
    SEPIA("ورقي", "Eye Care Sepia"),
    NIGHT_DIM("ليلي خافت", "Night Dim")
}

enum class MushafMode(val titleAr: String, val totalPages: Int) {
    MODE_16_LINES("مصحف ١٦ سطر", 581),
    MODE_604_PAGES("مصحف المدينة (٦٠٤ صفحة)", 604)
}

data class Surah(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val ayahCount: Int,
    val startPage604: Int,
    val startJuz: Int
) {
    val isMakki: Boolean get() = revelationType.equals("Meccan", ignoreCase = true)
}

data class Juz(
    val juzNumber: Int,
    val arabicName: String,
    val startSurahId: Int,
    val startSurahName: String,
    val startAyah: Int,
    val startPage604: Int
)

data class LineItem(
    val type: String, // "SURAH_HEADER", "BISMILLAH", "TEXT", "EMPTY"
    val text: String,
    val surah: Int,
    val surahName: String,
    val juz: Int,
    val page604: Int
)

data class Page16(
    val pageNumber: Int,
    val juz: Int,
    val surahName: String,
    val surahNumber: Int,
    val page604: Int,
    val lines: List<LineItem>
)

data class Page604Item(
    val type: String, // "SURAH_HEADER", "BISMILLAH", "AYAH"
    val surahNumber: Int = 1,
    val surahName: String = "",
    val ayahNumber: Int = 1,
    val globalNumber: Int = 1,
    val text: String = "",
    val juz: Int = 1,
    val endSymbol: String = ""
)

data class Page604(
    val pageNumber: Int,
    val items: List<Page604Item>
)

data class SearchResult(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val text: String,
    val page16: Int,
    val page604: Int,
    val juz: Int
)
