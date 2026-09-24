package com.example.ui.screens

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.QuranRepository
import com.example.ui.components.BismillahBanner
import com.example.ui.components.BookmarkRibbon
import com.example.ui.components.QuickJumpDialog
import com.example.ui.components.SurahHeaderBanner
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.viewmodel.QuranUiState
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.ScreenTab
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun ReaderScreen(
    uiState: QuranUiState,
    viewModel: QuranViewModel,
    modifier: Modifier = Modifier
) {
    val totalPages = uiState.mushafMode.totalPages
    val initialPage = (uiState.currentReaderPage - 1).coerceIn(0, totalPages - 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    val coroutineScope = rememberCoroutineScope()

    var showControlsBottomSheet by remember { mutableStateOf(false) }

    // Keep screen awake while reading
    val view = LocalView.current
    DisposableEffect(uiState.keepScreenAwake) {
        view.keepScreenOn = uiState.keepScreenAwake
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Synchronize page when user swipes
    LaunchedEffect(pagerState.currentPage) {
        val newPage = pagerState.currentPage + 1
        if (newPage != uiState.currentReaderPage) {
            viewModel.setReaderPage(newPage)
        }
    }

    // Synchronize pager if viewmodel page changes externally (e.g. Quick Jump)
    LaunchedEffect(uiState.currentReaderPage) {
        val targetIndex = (uiState.currentReaderPage - 1).coerceIn(0, totalPages - 1)
        if (pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    BackHandler {
        viewModel.selectTab(ScreenTab.HOME)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reader_screen")
    ) {
        // RTL Layout Provider for authentic Quran Right-to-Left swipe
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mushaf_pager"),
                key = { it }
            ) { pageIndex ->
                val pageNumber = pageIndex + 1

                // Page Turn Animation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
                            alpha = (1f - pageOffset * 0.25f).coerceIn(0.5f, 1f)
                            scaleX = (1f - pageOffset * 0.04f).coerceIn(0.96f, 1f)
                            scaleY = (1f - pageOffset * 0.04f).coerceIn(0.96f, 1f)
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.toggleImmersive()
                        }
                ) {
                    if (uiState.mushafMode == MushafMode.MODE_16_LINES) {
                        val page16Data = uiState.pages16.getOrNull(pageIndex)
                        val isBookmarked = uiState.bookmarks.any {
                            it.pageNumber == pageNumber && it.mushafMode == MushafMode.MODE_16_LINES.name
                        }

                        Mushaf16LinesPageView(
                            page = page16Data,
                            pageNumber = pageNumber,
                            fontScale = uiState.fontScale,
                            isBookmarked = isBookmarked
                        )
                    } else {
                        val page604Data = uiState.pages604.getOrNull(pageIndex)
                        val isBookmarked = uiState.bookmarks.any {
                            it.pageNumber == pageNumber && it.mushafMode == MushafMode.MODE_604_PAGES.name
                        }

                        Mushaf604PageView(
                            page = page604Data,
                            pageNumber = pageNumber,
                            fontScale = uiState.fontScale,
                            isBookmarked = isBookmarked
                        )
                    }
                }
            }
        }

        // Top App Bar (Toggled with Immersive Mode)
        AnimatedVisibility(
            visible = !uiState.isReaderImmersive,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)),
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderTopBar(
                currentPage = uiState.currentReaderPage,
                totalPages = totalPages,
                isBookmarked = viewModel.isCurrentPageBookmarked(),
                onBack = { viewModel.selectTab(ScreenTab.HOME) },
                onQuickJump = { viewModel.setQuickJumpOpen(true) },
                onToggleBookmark = { viewModel.toggleBookmarkCurrentPage() },
                onOpenControls = { showControlsBottomSheet = true }
            )
        }

        // Bottom Reading Bar (Toggled with Immersive Mode)
        AnimatedVisibility(
            visible = !uiState.isReaderImmersive,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReaderBottomBar(
                currentPage = uiState.currentReaderPage,
                totalPages = totalPages,
                onPreviousPage = {
                    if (pagerState.currentPage > 0) {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
                onNextPage = {
                    if (pagerState.currentPage < totalPages - 1) {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                onOpenControls = { showControlsBottomSheet = true }
            )
        }

        // Brightness Dim Overlay for Night Reading
        if (uiState.brightnessOverlay > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = uiState.brightnessOverlay))
            )
        }

        // Quick Jump Dialog
        if (uiState.isQuickJumpOpen) {
            QuickJumpDialog(
                surahs = uiState.surahs,
                juzList = uiState.juzList,
                mushafMode = uiState.mushafMode,
                currentPage = uiState.currentReaderPage,
                onDismiss = { viewModel.setQuickJumpOpen(false) },
                onJumpToPage = { targetPage ->
                    viewModel.setReaderPage(targetPage)
                }
            )
        }

        // Reader Controls Sheet (Theme, Font Size, Mode)
        if (showControlsBottomSheet) {
            ReaderControlsBottomSheet(
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { showControlsBottomSheet = false }
            )
        }
    }
}

/**
 * 16-Line Mushaf Page View: Formats the page into EXACTLY 16 lines with classical border.
 */
@Composable
fun Mushaf16LinesPageView(
    page: Page16?,
    pageNumber: Int,
    fontScale: Float,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("page_16_lines_$pageNumber")
    ) {
        // Bookmark Ribbon Indicator
        if (isBookmarked) {
            BookmarkRibbon(
                isBookmarked = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 2.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 52.dp, bottom = 48.dp, start = 6.dp, end = 6.dp)
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Page Header: Surah name (right), Islamic symbol (center), Juz name (left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = page?.surahName ?: "",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "۞",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "الجزء ${QuranRepository.toArabicNumber(page?.juz ?: 1)}",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = borderColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Exactly 16 Lines
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                val lines = page?.lines ?: emptyList()
                val total16Lines = if (lines.size >= 16) lines.take(16) else lines

                total16Lines.forEach { line ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when (line.type) {
                            "SURAH_HEADER" -> {
                                SurahHeaderBanner(title = line.text)
                            }
                            "BISMILLAH" -> {
                                BismillahBanner(text = line.text)
                            }
                            "TEXT" -> {
                                Text(
                                    text = line.text,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = (18f * fontScale).sp,
                                    lineHeight = (28f * fontScale).sp,
                                    textAlign = TextAlign.Center,
                                    color = textColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {
                                Spacer(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = borderColor
            )

            // Page Footer: Centered Page Number
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "— ${QuranRepository.toArabicNumber(pageNumber)} —",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 604-Page Madani Mushaf View.
 */
@Composable
fun Mushaf604PageView(
    page: Page604?,
    pageNumber: Int,
    fontScale: Float,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onBackground

    val firstAyah = page?.items?.firstOrNull { it.type == "AYAH" }
    val surahName = firstAyah?.surahName ?: ""
    val juzNum = firstAyah?.juz ?: 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("page_604_lines_$pageNumber")
    ) {
        if (isBookmarked) {
            BookmarkRibbon(
                isBookmarked = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 2.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 52.dp, bottom = 48.dp, start = 6.dp, end = 6.dp)
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = surahName,
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = "۞", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "الجزء ${QuranRepository.toArabicNumber(juzNum)}",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = borderColor)
            Spacer(modifier = Modifier.height(6.dp))

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                page?.items?.forEach { item ->
                    when (item.type) {
                        "SURAH_HEADER" -> SurahHeaderBanner(title = item.text)
                        "BISMILLAH" -> BismillahBanner(text = item.text)
                    }
                }

                // Concatenate ayahs for natural Quranic paragraph layout
                val ayahs = page?.items?.filter { it.type == "AYAH" } ?: emptyList()
                if (ayahs.isNotEmpty()) {
                    val fullText = buildString {
                        ayahs.forEach { a ->
                            append(a.text)
                            append(" ")
                            append(a.endSymbol)
                            append(" ")
                        }
                    }

                    Text(
                        text = fullText,
                        fontFamily = AmiriFontFamily,
                        fontSize = (19f * fontScale).sp,
                        lineHeight = (36f * fontScale).sp,
                        textAlign = TextAlign.Center,
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = borderColor)

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "— ${QuranRepository.toArabicNumber(pageNumber)} —",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ReaderTopBar(
    currentPage: Int,
    totalPages: Int,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onQuickJump: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenControls: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("reader_back_btn")
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Back")
            }

            Text(
                text = "صفحة $currentPage من $totalPages",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onQuickJump)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("reader_page_indicator")
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick Jump
                IconButton(
                    onClick = onQuickJump,
                    modifier = Modifier.testTag("reader_jump_btn")
                ) {
                    Icon(Icons.Default.Explore, contentDescription = "Jump")
                }

                // Bookmark Toggle
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("reader_bookmark_toggle")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Font & Theme Settings
                IconButton(
                    onClick = onOpenControls,
                    modifier = Modifier.testTag("reader_controls_btn")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Controls")
                }
            }
        }
    }
}

@Composable
fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenControls: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPreviousPage,
                enabled = currentPage > 1,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("reader_prev_btn")
            ) {
                Text("السابقة", fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onOpenControls) {
                Icon(Icons.Default.FormatSize, contentDescription = "Font size")
            }

            Button(
                onClick = onNextPage,
                enabled = currentPage < totalPages,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("reader_next_btn")
            ) {
                Text("التالية", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderControlsBottomSheet(
    uiState: QuranUiState,
    viewModel: QuranViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "خيارات القراءة السريعة",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reading Theme Row
            Text(
                text = "لون الخلفية وحماية العين",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingThemeMode.values().forEach { mode ->
                    val isSelected = uiState.readingTheme == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setReadingTheme(mode) },
                        label = { Text(mode.titleAr, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mushaf Mode
            Text(
                text = "نمط المصحف",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MushafMode.values().forEach { mode ->
                    val isSelected = uiState.mushafMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setMushafMode(mode) },
                        label = { Text(mode.titleAr, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font Scale Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "حجم الخط",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${(uiState.fontScale * 100).toInt()}%",
                    fontSize = 13.sp
                )
            }
            Slider(
                value = uiState.fontScale,
                onValueChange = { viewModel.setFontScale(it) },
                valueRange = 0.85f..1.4f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Brightness Overlay Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تعتيم ليلي إضافي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${(uiState.brightnessOverlay * 100).toInt()}%",
                    fontSize = 13.sp
                )
            }
            Slider(
                value = uiState.brightnessOverlay,
                onValueChange = { viewModel.setBrightnessOverlay(it) },
                valueRange = 0f..0.6f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
