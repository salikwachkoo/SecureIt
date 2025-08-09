package com.mohammadsalik.secureit.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mohammadsalik.secureit.core.preferences.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, contentDescription = null) } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            // Theme
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Theme", modifier = Modifier.weight(1f))
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = uiState.themeMode == ThemeMode.System,
                        onClick = { viewModel.setThemeMode(ThemeMode.System) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        label = { Text("System") }
                    )
                    SegmentedButton(
                        selected = uiState.themeMode == ThemeMode.Light,
                        onClick = { viewModel.setThemeMode(ThemeMode.Light) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        label = { Text("Light") }
                    )
                    SegmentedButton(
                        selected = uiState.themeMode == ThemeMode.Dark,
                        onClick = { viewModel.setThemeMode(ThemeMode.Dark) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        label = { Text("Dark") }
                    )
                }
            }
            // Dynamic color
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dynamic color", modifier = Modifier.weight(1f))
                Switch(checked = uiState.dynamicColor, onCheckedChange = { viewModel.setDynamicColor(it) })
            }
            // Text scale
            Column {
                Text("Text size")
                Slider(value = uiState.textScale, onValueChange = { viewModel.setTextScale(it) }, valueRange = 0.85f..1.5f)
            }
            // Reduce motion
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reduce motion", modifier = Modifier.weight(1f))
                Switch(checked = uiState.reduceMotion, onCheckedChange = { viewModel.setReduceMotion(it) })
            }
        }
    }
}
