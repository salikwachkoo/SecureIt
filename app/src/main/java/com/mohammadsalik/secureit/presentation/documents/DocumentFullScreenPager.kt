package com.mohammadsalik.secureit.presentation.documents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentFullScreenPager(
    documentId: Long,
    startPage: Int,
    onBack: () -> Unit,
    viewModel: DocumentViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(documentId) { if (uiState.document?.id != documentId) viewModel.load(documentId) }

    val pageCount = uiState.previews.size
    val pagerState = rememberPagerState(initialPage = startPage.coerceAtMost((pageCount - 1).coerceAtLeast(0))) { pageCount }

    val scales = remember(pageCount) { mutableStateListOf<Float>().apply { repeat(pageCount) { add(1f) } } }
    val offsets = remember(pageCount) { mutableStateListOf<Offset>().apply { repeat(pageCount) { add(Offset.Zero) } } }

    LaunchedEffect(pageCount) {
        if (scales.size != pageCount) { scales.clear(); repeat(pageCount) { scales.add(1f) }; offsets.clear(); repeat(pageCount) { offsets.add(Offset.Zero) } }
    }

    val currentScale by derivedStateOf { if (pageCount == 0) 1f else scales.getOrElse(pagerState.currentPage) { 1f } }

    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            title = {
                val title = uiState.document?.title ?: "Document"
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        )
    }) { padding ->
        if (uiState.previews.isNotEmpty()) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = currentScale <= 1f,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val bmp = uiState.previews[page]
                    if (bmp != null) {
                        var scale by remember(page) { mutableStateOf(scales[page]) }
                        var offset by remember(page) { mutableStateOf(offsets[page]) }
                        val minZoom = 1f
                        val maxZoom = 5f
                        var pinchActive by remember(page) { mutableStateOf(false) }

                        LaunchedEffect(scale, offset) { scales[page] = scale; offsets[page] = offset }

                        // Track pointer count to enable pinch-to-zoom starting from 1x
                        val pointerTracker = Modifier.pointerInput(page) {
                            awaitPointerEventScope {
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val pressed = ev.changes.count { it.pressed }
                                    pinchActive = pressed >= 2
                                }
                            }
                        }

                        val transformModifier = if (pinchActive || scale > minZoom) Modifier.pointerInput(page, scale) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(minZoom, maxZoom)
                                val applyPan = if (newScale > minZoom) pan else Offset.Zero
                                scale = newScale
                                offset += applyPan
                            }
                        } else Modifier

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(pointerTracker)
                                .then(transformModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                AssistChip(onClick = {}, label = { Text("${pagerState.currentPage + 1} / ${pageCount.coerceAtLeast(1)}") }, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp))
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) CircularProgressIndicator() else Text(uiState.error ?: "No preview available")
            }
        }
    }
}