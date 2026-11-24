package com.example

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.F1MainApp
import com.example.ui.F1ViewModel
import com.example.ui.components.LocalListPrefetchStrategy
import com.example.ui.theme.MyApplicationTheme

@OptIn(ExperimentalFoundationApi::class)
private val noPrefetch = object : LazyListPrefetchStrategy {
    override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) = Unit
    override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) = Unit
    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) = Unit
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ApexTestContent(vm: F1ViewModel, dark: Boolean = true) {
    CompositionLocalProvider(LocalListPrefetchStrategy provides noPrefetch) {
        MyApplicationTheme(darkTheme = dark) { F1MainApp(vm) }
    }
}
