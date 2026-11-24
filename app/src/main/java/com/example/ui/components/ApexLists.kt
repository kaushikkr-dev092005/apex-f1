package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Default platform prefetch in the app; injectable scheduling for deterministic host tests. */
@OptIn(ExperimentalFoundationApi::class)
val LocalListPrefetchStrategy = staticCompositionLocalOf<LazyListPrefetchStrategy?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberApexListState(): LazyListState {
    val strategy = LocalListPrefetchStrategy.current
    return if (strategy == null) rememberLazyListState() else rememberLazyListState(prefetchStrategy = strategy)
}

@Composable
fun ApexLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(modifier, state = rememberApexListState(), contentPadding = contentPadding, verticalArrangement = verticalArrangement, content = content)
}

@Composable
fun ApexLazyRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: LazyListScope.() -> Unit
) {
    LazyRow(modifier, state = rememberApexListState(), contentPadding = contentPadding, horizontalArrangement = horizontalArrangement, content = content)
}
