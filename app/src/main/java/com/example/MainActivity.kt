package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.QuickJumpDialog
import com.example.ui.screens.*
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.QuranAppTheme
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: QuranViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            QuranAppTheme(readingTheme = uiState.readingTheme) {
                // Wrap in RTL Layout Direction for authentic Arabic experience
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainContent(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    uiState: com.example.ui.viewmodel.QuranUiState,
    viewModel: QuranViewModel
) {
    if (uiState.currentTab == ScreenTab.READER) {
        ReaderScreen(
            uiState = uiState,
            viewModel = viewModel
        )
        return
    }

    if (uiState.isSearchOpen) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) { innerPadding ->
            SearchScreen(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.currentTab) {
                            ScreenTab.HOME -> "القرآن الكريم"
                            ScreenTab.SURAHS -> "فهرس السور"
                            ScreenTab.JUZ -> "الأجزاء"
                            ScreenTab.BOOKMARKS -> "العلامات المرجعية"
                            ScreenTab.SETTINGS -> "الإعدادات"
                            else -> "القرآن الكريم"
                        },
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    // Search Action
                    IconButton(
                        onClick = { viewModel.setSearchOpen(true) },
                        modifier = Modifier.testTag("action_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    // Quick Jump Action
                    IconButton(
                        onClick = { viewModel.setQuickJumpOpen(true) },
                        modifier = Modifier.testTag("action_quick_jump")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Quick Jump"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Home Tab
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.HOME,
                    onClick = { viewModel.selectTab(ScreenTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("الرئيسية", fontFamily = AmiriFontFamily, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_home")
                )

                // Surahs Tab
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.SURAHS,
                    onClick = { viewModel.selectTab(ScreenTab.SURAHS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.SURAHS) Icons.Filled.FormatListNumbered else Icons.Outlined.FormatListNumbered,
                            contentDescription = "Surahs"
                        )
                    },
                    label = { Text("السور", fontFamily = AmiriFontFamily, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_surahs")
                )

                // Juz Tab
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.JUZ,
                    onClick = { viewModel.selectTab(ScreenTab.JUZ) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.JUZ) Icons.Filled.AutoStories else Icons.Outlined.AutoStories,
                            contentDescription = "Juz"
                        )
                    },
                    label = { Text("الأجزاء", fontFamily = AmiriFontFamily, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_juz")
                )

                // Bookmarks Tab
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.BOOKMARKS,
                    onClick = { viewModel.selectTab(ScreenTab.BOOKMARKS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.BOOKMARKS) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmarks"
                        )
                    },
                    label = { Text("الإشارات", fontFamily = AmiriFontFamily, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_bookmarks")
                )

                // Settings Tab
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.SETTINGS,
                    onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("الإعدادات", fontFamily = AmiriFontFamily, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                ScreenTab.HOME -> HomeScreen(uiState = uiState, viewModel = viewModel)
                ScreenTab.SURAHS -> SurahListScreen(uiState = uiState, viewModel = viewModel)
                ScreenTab.JUZ -> JuzListScreen(uiState = uiState, viewModel = viewModel)
                ScreenTab.BOOKMARKS -> BookmarksScreen(uiState = uiState, viewModel = viewModel)
                ScreenTab.SETTINGS -> SettingsScreen(uiState = uiState, viewModel = viewModel)
                else -> HomeScreen(uiState = uiState, viewModel = viewModel)
            }

            // Quick Jump Dialog (when opened from any screen top bar)
            if (uiState.isQuickJumpOpen) {
                QuickJumpDialog(
                    surahs = uiState.surahs,
                    juzList = uiState.juzList,
                    mushafMode = uiState.mushafMode,
                    currentPage = uiState.currentReaderPage,
                    onDismiss = { viewModel.setQuickJumpOpen(false) },
                    onJumpToPage = { page ->
                        viewModel.openReader(page)
                    }
                )
            }
        }
    }
}
