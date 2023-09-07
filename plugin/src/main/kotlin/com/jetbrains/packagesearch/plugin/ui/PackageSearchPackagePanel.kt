package com.jetbrains.packagesearch.plugin.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.dp
import com.jetbrains.packagesearch.plugin.core.data.PackageSearchModule
import com.jetbrains.packagesearch.plugin.ui.models.InfoBoxDetail
import com.jetbrains.packagesearch.plugin.ui.models.SearchData
import com.jetbrains.packagesearch.plugin.ui.models.buildPackageGroups
import com.jetbrains.packagesearch.plugin.ui.models.buildSearchData
import com.jetbrains.packagesearch.plugin.ui.sections.infobox.PackageSearchInfoBox
import com.jetbrains.packagesearch.plugin.ui.sections.modulesbox.PackageSearchCentralPanel
import com.jetbrains.packagesearch.plugin.ui.sections.treebox.PackageSearchModulesTree
import com.jetbrains.packagesearch.plugin.utils.logError
import java.awt.Cursor
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.jewel.Text
import org.jetbrains.jewel.foundation.tree.Tree
import org.jetbrains.packagesearch.api.v3.http.PackageSearchApiClient
import org.jetbrains.packagesearch.api.v3.http.SearchPackagesRequest

@Composable
fun PackageSearchPackagePanel(
    isInfoBoxOpen: Boolean,
    tree: Tree<PackageSearchModule>,
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val splitPaneState = rememberSplitPaneState(.20f)
    val innerSplitPaneState = rememberSplitPaneState(.80f)
    val splitterColor = LocalScrollbarStyle.current.unhoverColor
    val infoBoxScrollState = rememberScrollState()

    var infoBoxDetail by remember { mutableStateOf<InfoBoxDetail?>(null) }
    var searchResults by remember { mutableStateOf<SearchData.Results>(SearchData.Results.Empty) }

    var selectedModules by remember { mutableStateOf<List<PackageSearchModule>>(emptyList()) }

    val packageGroups by derivedStateOf {
        buildPackageGroups(searchQuery) {
            setSearchResults(searchResults)
            setLocal(selectedModules)
        }
    }

    @Composable
    fun PackageSearchCentralPanel() {
        if (selectedModules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Select one or more modules on the left to show declared dependencies")
            }
        }
        else PackageSearchCentralPanel(
            isLoading = isSearching,
            isInfoBoxOpen = isInfoBoxOpen,
            packageGroups = packageGroups,
            searchQuery = searchQuery,
            onElementClick = { infoBoxDetail = it },
            onSearchQueryChange = { searchQuery = it },
        )
    }

    HorizontalSplitPane(Modifier.fillMaxSize(), splitPaneState) {
        first(100.dp) {
            Column(Modifier.fillMaxSize()) {
                PackageSearchModulesTree(tree) { selectedModules = it }
            }
        }
        defaultPKGSSplitter(splitterColor, cursor = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
        second(500.dp) {
            if (isInfoBoxOpen) {
                HorizontalSplitPane(Modifier.fillMaxSize(), innerSplitPaneState) {
                    first(minSize = 100.dp) {
                        PackageSearchCentralPanel()
                    }
                    defaultPKGSSplitter(splitterColor, cursor = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                    second(160.dp) {
                        Box {
                            Column(
                                modifier = Modifier.verticalScroll(infoBoxScrollState)
                            ) {
                                PackageSearchInfoBox(infoBoxDetail, selectedModules)
                            }
                            Box(modifier = Modifier.matchParentSize()) {
                                VerticalScrollbar(
                                    rememberScrollbarAdapter(infoBoxScrollState),
                                    modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }
                }
            } else PackageSearchCentralPanel()
        }
    }

    val json = LocalJson.current
    val apiClient = LocalPackageSearchApiClient.current

    LaunchedEffect(selectedModules, searchQuery) {
        delay(1.seconds) // debounce
        isSearching = true
        searchResults = when (val searchData = buildSearchData(selectedModules, searchQuery)) {
            SearchData.Empty -> SearchData.Results.Empty
            is SearchData.SingleBaseModule ->
                searchData.withResults(apiClient.trySearchPackages(json, searchData.searchParameters))

            is SearchData.MultipleModules ->
                searchData.withResults(apiClient.trySearchPackages(json, searchData.searchParameters))

            is SearchData.SingleWithVariantsModule -> searchData.withResults(
                results = searchData.searchForVariant
                    .map { it to async { apiClient.trySearchPackages(json, it.searchParameters) } }
                    .map { (searchForVariant, search) ->
                        searchForVariant.withResults(search.await())
                    }
            )
        }
        isSearching = false
    }
}

private suspend fun PackageSearchApiClient.trySearchPackages(json: Json, request: SearchPackagesRequest) =
    runCatching { searchPackages(request) }
        .onFailure {
            logError(it) {
                """
Error while searching. Request:
|----------------
|${json.encodeToString(request)}
|----------------
""".trimIndent()
            }
        }
        .getOrDefault(emptyList())