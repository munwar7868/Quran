package com.example

import com.example.data.model.MushafMode
import com.example.data.model.ReadingThemeMode
import com.example.data.repository.QuranRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranUnitTest {

    @Test
    fun testArabicNormalization() {
        val raw = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val normalized = QuranRepository.normalizeArabic(raw)
        assertEquals("بسم الله الرحمن الرحيم", normalized)

        val hamzaTest = "أحمد إبراهيم آمنة"
        val hamzaNorm = QuranRepository.normalizeArabic(hamzaTest)
        assertEquals("احمد ابراهيم امنه", hamzaNorm)
    }

    @Test
    fun testArabicNumberConversion() {
        assertEquals("١", QuranRepository.toArabicNumber(1))
        assertEquals("١١٤", QuranRepository.toArabicNumber(114))
        assertEquals("٦٠٤", QuranRepository.toArabicNumber(604))
        assertEquals("٥٨١", QuranRepository.toArabicNumber(581))
    }

    @Test
    fun testMushafModes() {
        assertEquals(581, MushafMode.MODE_16_LINES.totalPages)
        assertEquals(604, MushafMode.MODE_604_PAGES.totalPages)
    }

    @Test
    fun testReadingThemeModes() {
        assertEquals(4, ReadingThemeMode.values().size)
    }
}
