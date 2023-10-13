package com.jetbrains.packagesearch.plugin.ui.sections.modulesbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.intellij.ui.JBColor
import com.jetbrains.packagesearch.plugin.ui.model.InfoBoxDetail
import com.jetbrains.packagesearch.plugin.ui.model.PackageGroup
import org.jetbrains.jewel.Divider
import org.jetbrains.jewel.IndeterminateHorizontalProgressBar
import org.jetbrains.jewel.Orientation
import org.jetbrains.jewel.bridge.toComposeColor
import org.jetbrains.jewel.intui.standalone.IntUiTheme

@Composable
fun PackageSearchCentralPanel(
    isLoading: Boolean,
    isInfoBoxOpen: Boolean,
    packageGroups: List<PackageGroup>,
    searchQuery: String,
    onElementClick: (InfoBoxDetail?) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
) {
    val borderColor by remember(IntUiTheme.isDark) { mutableStateOf(JBColor.border().toComposeColor()) }

    Column {
        SearchRow(
            searchQuery = searchQuery,
            searchResultsCount = packageGroups.filterIsInstance<PackageGroup.Remote>()
                .sumOf { it.size },
            onSearchQueryChange = onSearchQueryChange,
        )
        Divider(orientation = Orientation.Horizontal, Modifier.fillMaxWidth(), color = borderColor)

        Box {
            when {
                packageGroups.isEmpty() && !isLoading -> NoResultsToShow()
                packageGroups.isNotEmpty() -> PackageSearchPackageList(
                    packageGroups = packageGroups,
                    isInfoBoxOpen = isInfoBoxOpen,
                    onElementClick = onElementClick,
                )
            }
            if (isLoading) IndeterminateHorizontalProgressBar(Modifier.fillMaxWidth())
        }

    }
}
