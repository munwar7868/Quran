package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.data.db.BookmarkEntity
import com.example.data.db.ReadingHistoryEntity
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

class QuranRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.quranDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)

    // Memory caches for fast UI response
    private var cachedSurahs: List<Surah>? = null
    private var cachedJuzList: List<Juz>? = null
    private var cachedPages16: List<Page16>? = null
    private var cachedPages604: List<Page604>? = null
    private var searchIndex: List<SearchResult>? = null

    val bookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()
    val lastRead: Flow<ReadingHistoryEntity?> = dao.getLastRead()

    fun isPageBookmarked(pageNumber: Int, mode: String): Flow<Boolean> {
        return dao.isPageBookmarked(pageNumber, mode)
    }

    suspend fun addBookmark(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        pageNumber: Int,
        mushafMode: String,
        previewText: String
    ) {
        dao.insertBookmark(
            BookmarkEntity(
                surahNumber = surahNumber,
                surahName = surahName,
                ayahNumber = ayahNumber,
                pageNumber = pageNumber,
                mushafMode = mushafMode,
                previewText = previewText
            )
        )
    }

    suspend fun removeBookmarkById(id: Long) {
        dao.deleteBookmarkById(id)
    }

    suspend fun removeBookmarkByPage(pageNumber: Int, mushafMode: String) {
        dao.deleteBookmarkByPage(pageNumber, mushafMode)
    }

    suspend fun saveReadingPosition(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        pageNumber: Int,
        mushafMode: String,
        juz: Int
    ) {
        dao.saveLastRead(
            ReadingHistoryEntity(
                id = 1,
                surahNumber = surahNumber,
                surahName = surahName,
                ayahNumber = ayahNumber,
                pageNumber = pageNumber,
                mushafMode = mushafMode,
                juz = juz
            )
        )
    }

    // Preferences
    fun getThemeMode(): ReadingThemeMode {
        val name = prefs.getString("reading_theme", ReadingThemeMode.LIGHT.name)
        return try {
            ReadingThemeMode.valueOf(name ?: ReadingThemeMode.LIGHT.name)
        } catch (_: Exception) {
            ReadingThemeMode.LIGHT
        }
    }

    fun setThemeMode(mode: ReadingThemeMode) {
        prefs.edit().putString("reading_theme", mode.name).apply()
    }

    fun getMushafMode(): MushafMode {
        val name = prefs.getString("mushaf_mode", MushafMode.MODE_16_LINES.name)
        return try {
            MushafMode.valueOf(name ?: MushafMode.MODE_16_LINES.name)
        } catch (_: Exception) {
            MushafMode.MODE_16_LINES
        }
    }

    fun setMushafMode(mode: MushafMode) {
        prefs.edit().putString("mushaf_mode", mode.name).apply()
    }

    fun getFontScale(): Float {
        return prefs.getFloat("font_scale", 1.0f)
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat("font_scale", scale).apply()
    }

    fun getBrightnessOverlay(): Float {
        return prefs.getFloat("brightness_overlay", 0.0f)
    }

    fun setBrightnessOverlay(overlay: Float) {
        prefs.edit().putFloat("brightness_overlay", overlay).apply()
    }

    fun isKeepScreenAwake(): Boolean {
        return prefs.getBoolean("keep_screen_awake", true)
    }

    fun setKeepScreenAwake(keep: Boolean) {
        prefs.edit().putBoolean("keep_screen_awake", keep).apply()
    }

    // Data Loaders
    suspend fun getSurahs(): List<Surah> = withContext(Dispatchers.IO) {
        cachedSurahs?.let { return@withContext it }
        val list = mutableListOf<Surah>()
        try {
            val jsonStr = context.assets.open("surahs_meta.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Surah(
                        number = obj.getInt("number"),
                        name = obj.getString("name"),
                        englishName = obj.getString("englishName"),
                        englishNameTranslation = obj.getString("englishNameTranslation"),
                        revelationType = obj.getString("revelationType"),
                        ayahCount = obj.getInt("ayahCount"),
                        startPage604 = obj.getInt("startPage604"),
                        startJuz = obj.getInt("startJuz")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedSurahs = list
        list
    }

    suspend fun getJuzList(): List<Juz> = withContext(Dispatchers.IO) {
        cachedJuzList?.let { return@withContext it }
        val list = mutableListOf<Juz>()
        try {
            val jsonStr = context.assets.open("juz_meta.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Juz(
                        juzNumber = obj.getInt("juzNumber"),
                        arabicName = obj.getString("arabicName"),
                        startSurahId = obj.getInt("startSurahId"),
                        startSurahName = obj.getString("startSurahName"),
                        startAyah = obj.getInt("startAyah"),
                        startPage604 = obj.getInt("startPage604")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedJuzList = list
        list
    }

    suspend fun getPages16(): List<Page16> = withContext(Dispatchers.IO) {
        cachedPages16?.let { return@withContext it }
        val list = mutableListOf<Page16>()
        try {
            val rawStream = context.assets.open("quran_16lines.json.gz")
            val gzipStream = GZIPInputStream(rawStream)
            val jsonStr = BufferedReader(InputStreamReader(gzipStream, Charsets.UTF_8)).use { it.readText() }
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val linesArr = obj.getJSONArray("lines")
                val lineList = mutableListOf<LineItem>()
                for (j in 0 until linesArr.length()) {
                    val lObj = linesArr.getJSONObject(j)
                    lineList.add(
                        LineItem(
                            type = lObj.getString("type"),
                            text = lObj.optString("text", ""),
                            surah = lObj.optInt("surah", 1),
                            surahName = lObj.optString("surahName", ""),
                            juz = lObj.optInt("juz", 1),
                            page604 = lObj.optInt("page604", 1)
                        )
                    )
                }
                list.add(
                    Page16(
                        pageNumber = obj.getInt("pageNumber"),
                        juz = obj.getInt("juz"),
                        surahName = obj.getString("surahName"),
                        surahNumber = obj.getInt("surahNumber"),
                        page604 = obj.getInt("page604"),
                        lines = lineList
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedPages16 = list
        list
    }

    suspend fun getPages604(): List<Page604> = withContext(Dispatchers.IO) {
        cachedPages604?.let { return@withContext it }
        val list = mutableListOf<Page604>()
        try {
            val rawStream = context.assets.open("quran_604pages.json.gz")
            val gzipStream = GZIPInputStream(rawStream)
            val jsonStr = BufferedReader(InputStreamReader(gzipStream, Charsets.UTF_8)).use { it.readText() }
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val itemsArr = obj.getJSONArray("items")
                val itemList = mutableListOf<Page604Item>()
                for (j in 0 until itemsArr.length()) {
                    val itObj = itemsArr.getJSONObject(j)
                    itemList.add(
                        Page604Item(
                            type = itObj.getString("type"),
                            surahNumber = itObj.optInt("surahNumber", 1),
                            surahName = itObj.optString("surahName", ""),
                            ayahNumber = itObj.optInt("ayahNumber", 1),
                            globalNumber = itObj.optInt("globalNumber", 1),
                            text = itObj.optString("text", ""),
                            juz = itObj.optInt("juz", 1),
                            endSymbol = itObj.optString("endSymbol", "")
                        )
                    )
                }
                list.add(
                    Page604(
                        pageNumber = obj.getInt("pageNumber"),
                        items = itemList
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedPages604 = list
        list
    }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val normalizedQuery = normalizeArabic(trimmed).lowercase()

        // Build index on demand from full Tanzil dataset
        if (searchIndex == null) {
            val indexList = mutableListOf<SearchResult>()
            try {
                val rawStream = context.assets.open("quran_uthmani.json.gz")
                val gzipStream = GZIPInputStream(rawStream)
                val jsonStr = BufferedReader(InputStreamReader(gzipStream, Charsets.UTF_8)).use { it.readText() }
                val surahsArr = JSONArray(jsonStr)
                for (sIdx in 0 until surahsArr.length()) {
                    val sObj = surahsArr.getJSONObject(sIdx)
                    val sNum = sObj.getInt("number")
                    val sName = sObj.getString("name")
                    val ayahsArr = sObj.getJSONArray("ayahs")
                    for (aIdx in 0 until ayahsArr.length()) {
                        val aObj = ayahsArr.getJSONObject(aIdx)
                        indexList.add(
                            SearchResult(
                                surahNumber = sNum,
                                surahName = sName,
                                ayahNumber = aObj.getInt("numberInSurah"),
                                text = aObj.getString("text"),
                                page16 = ((aObj.getInt("page") * 581) / 604).coerceIn(1, 581),
                                page604 = aObj.getInt("page"),
                                juz = aObj.getInt("juz")
                            )
                        )
                    }
                }
                searchIndex = indexList
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList()
            }
        }

        // Filter index by normalized text or surah name
        searchIndex?.filter { item ->
            normalizeArabic(item.text).contains(normalizedQuery, ignoreCase = true) ||
                    normalizeArabic(item.surahName).contains(normalizedQuery, ignoreCase = true)
        }?.take(100) ?: emptyList()
    }

    companion object {
        fun normalizeArabic(text: String): String {
            return text
                .replace(Regex("[\\u064B-\\u065F\\u0670]"), "") // Remove harakat/tashkeel
                .replace(Regex("[أإآٱ]"), "ا") // Normalize alef
                .replace("ة", "ه") // Normalize taa marbuta
                .replace("ى", "ي") // Normalize alif maqsoora
                .replace("ـ", "") // Remove tatweel
                .trim()
        }

        fun toArabicNumber(number: Int): String {
            val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
            val str = number.toString()
            val sb = StringBuilder()
            for (c in str) {
                if (c in '0'..'9') {
                    sb.append(arabicDigits[c - '0'])
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}
