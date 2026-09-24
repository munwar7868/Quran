package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Juz
import com.example.data.model.MushafMode
import com.example.data.repository.QuranRepository
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.viewmodel.QuranUiState
import com.example.ui.viewmodel.QuranViewModel

@Composable
fun JuzListScreen(
    uiState: QuranUiState,
    viewModel: QuranViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("juz_list_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(uiState.juzList, key = { it.juzNumber }) { juz ->
            JuzItemCard(
                juz = juz,
                mushafMode = uiState.mushafMode,
                onClick = {
                    val targetPage = if (uiState.mushafMode == MushafMode.MODE_16_LINES) {
                        ((juz.startPage604 * 581) / 604).coerceIn(1, 581)
                    } else {
                        juz.startPage604
                    }
                    viewModel.openReader(targetPage)
                }
            )
        }
    }
}

@Composable
fun JuzItemCard(
    juz: Juz,
    mushafMode: MushafMode,
    onClick: () -> Unit
) {
    val targetPage = if (mushafMode == MushafMode.MODE_16_LINES) {
        ((juz.startPage604 * 581) / 604).coerceIn(1, 581)
    } else {
        juz.startPage604
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("juz_item_${juz.juzNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = QuranRepository.toArabicNumber(juz.juzNumber),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = AmiriFontFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "الجزء ${QuranRepository.toArabicNumber(juz.juzNumber)}: ${juz.arabicName}",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "بداية: ${juz.startSurahName} (الآية ${juz.startAyah})",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "ص $targetPage",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
