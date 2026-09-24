package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BookmarkEntity
import com.example.data.db.ReadingHistoryEntity
import com.example.data.model.*
import com.example.data.repository.QuranRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenTab {
    HOME,
    SURAHS,
    JUZ,
    BOOKMARKS,
    SETTINGS,
    READER
}

data class QuranUiState(
    val currentTab: ScreenTab = ScreenTab.HOME,
    val surahs: List<Surah> = emptyList(),
    val juzList: List<Juz> = emptyList(),
    val pages16: List<Page16> = emptyList(),
    val pages604: List<Page604> = emptyList(),
    val lastRead: ReadingHistoryEntity? = null,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val readingTheme: ReadingThemeMode = ReadingThemeMode.LIGHT,
    val mushafMode: MushafMode = MushafMode.MODE_16_LINES,
    val currentReaderPage: Int = 1,
    val fontScale: Float = 1.0f,
    val brightnessOverlay: Float = 0.0f,
    val keepScreenAwake: Boolean = true,
    val isReaderImmersive: Boolean = false,
    val isQuickJumpOpen: Boolean = false,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingData: Boolean = true
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(application)

    private val _uiState = MutableStateFlow(
        QuranUiState(
            readingTheme = repository.getThemeMode(),
            mushafMode = repository.getMushafMode(),
            fontScale = repository.getFontScale(),
            brightnessOverlay = repository.getBrightnessOverlay(),
            keepScreenAwake = repository.isKeepScreenAwake()
        )
    )
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    init {
        // Collect Bookmarks from Room
        viewModelScope.launch {
            repository.bookmarks.collect { list ->
                _uiState.update { it.copy(bookmarks = list) }
            }
        }

        // Collect Last Read from Room
        viewModelScope.launch {
            repository.lastRead.collect { history ->
                _uiState.update { it.copy(lastRead = history) }
            }
        }

        // Load Quran Data
        viewModelScope.launch {
            val surahs = repository.getSurahs()
            val juzList = repository.getJuzList()
            val pages16 = repository.getPages16()
            val pages604 = repository.getPages604()

            _uiState.update {
                it.copy(
                    surahs = surahs,
                    juzList = juzList,
                    pages16 = pages16,
                    pages604 = pages604,
                    isLoadingData = false
                )
            }
        }
    }

    fun selectTab(tab: ScreenTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun openReader(page: Int, mode: MushafMode = _uiState.value.mushafMode) {
        val total = mode.totalPages
        val safePage = page.coerceIn(1, total)
        _uiState.update {
            it.copy(
                currentReaderPage = safePage,
                mushafMode = mode,
                currentTab = ScreenTab.READER
            )
        }
    }

    fun setReaderPage(page: Int) {
        val maxPages = _uiState.value.mushafMode.totalPages
        val safePage = page.coerceIn(1, maxPages)
        _uiState.update { it.copy(currentReaderPage = safePage) }

        // Automatically update reading position in Room
        saveCurrentPosition(safePage)
    }

    private fun saveCurrentPosition(page: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val surahNum: Int
            val surahName: String
            val juz: Int

            if (state.mushafMode == MushafMode.MODE_16_LINES) {
                val pageData = state.pages16.getOrNull(page - 1)
                surahNum = pageData?.surahNumber ?: 1
                surahName = pageData?.surahName ?: "الفاتحة"
                juz = pageData?.juz ?: 1
            } else {
                val pageData = state.pages604.getOrNull(page - 1)
                val firstAyah = pageData?.items?.firstOrNull { it.type == "AYAH" }
                surahNum = firstAyah?.surahNumber ?: 1
                surahName = firstAyah?.surahName ?: "الفاتحة"
                juz = firstAyah?.juz ?: 1
            }

            repository.saveReadingPosition(
                surahNumber = surahNum,
                surahName = surahName,
                ayahNumber = 1,
                pageNumber = page,
                mushafMode = state.mushafMode.name,
                juz = juz
            )
        }
    }

    fun toggleBookmarkCurrentPage() {
        viewModelScope.launch {
            val state = _uiState.value
            val page = state.currentReaderPage
            val modeName = state.mushafMode.name
            val existing = state.bookmarks.find { it.pageNumber == page && it.mushafMode == modeName }

            if (existing != null) {
                repository.removeBookmarkById(existing.id)
            } else {
                val surahNum: Int
                val surahName: String
                val preview: String

                if (state.mushafMode == MushafMode.MODE_16_LINES) {
                    val pageData = state.pages16.getOrNull(page - 1)
                    surahNum = pageData?.surahNumber ?: 1
                    surahName = pageData?.surahName ?: "الفاتحة"
                    preview = pageData?.lines?.firstOrNull { it.type == "TEXT" }?.text?.take(45) ?: surahName
                } else {
                    val pageData = state.pages604.getOrNull(page - 1)
                    val firstAyah = pageData?.items?.firstOrNull { it.type == "AYAH" }
                    surahNum = firstAyah?.surahNumber ?: 1
                    surahName = firstAyah?.surahName ?: "الفاتحة"
                    preview = firstAyah?.text?.take(45) ?: surahName
                }

                repository.addBookmark(
                    surahNumber = surahNum,
                    surahName = surahName,
                    ayahNumber = 1,
                    pageNumber = page,
                    mushafMode = modeName,
                    previewText = preview
                )
            }
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmarkById(id)
        }
    }

    fun isCurrentPageBookmarked(): Boolean {
        val state = _uiState.value
        val page = state.currentReaderPage
        val mode = state.mushafMode.name
        return state.bookmarks.any { it.pageNumber == page && it.mushafMode == mode }
    }

    fun setReadingTheme(theme: ReadingThemeMode) {
        repository.setThemeMode(theme)
        _uiState.update { it.copy(readingTheme = theme) }
    }

    fun setMushafMode(mode: MushafMode) {
        repository.setMushafMode(mode)
        val currentP = _uiState.value.currentReaderPage
        val convertedPage = if (mode == MushafMode.MODE_16_LINES) {
            ((currentP * 581) / 604).coerceIn(1, 581)
        } else {
            ((currentP * 604) / 581).coerceIn(1, 604)
        }
        _uiState.update { it.copy(mushafMode = mode, currentReaderPage = convertedPage) }
    }

    fun setFontScale(scale: Float) {
        repository.setFontScale(scale)
        _uiState.update { it.copy(fontScale = scale) }
    }

    fun setBrightnessOverlay(overlay: Float) {
        repository.setBrightnessOverlay(overlay)
        _uiState.update { it.copy(brightnessOverlay = overlay) }
    }

    fun setKeepScreenAwake(keep: Boolean) {
        repository.setKeepScreenAwake(keep)
        _uiState.update { it.copy(keepScreenAwake = keep) }
    }

    fun toggleImmersive() {
        _uiState.update { it.copy(isReaderImmersive = !it.isReaderImmersive) }
    }

    fun setQuickJumpOpen(open: Boolean) {
        _uiState.update { it.copy(isQuickJumpOpen = open) }
    }

    fun setSearchOpen(open: Boolean) {
        _uiState.update { it.copy(isSearchOpen = open, searchQuery = "", searchResults = emptyList()) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = true) }
        viewModelScope.launch {
            val results = repository.search(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }
}
