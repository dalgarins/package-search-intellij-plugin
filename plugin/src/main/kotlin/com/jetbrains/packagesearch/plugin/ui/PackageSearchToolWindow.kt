package com.jetbrains.packagesearch.plugin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.ui.JBColor
import com.jetbrains.packagesearch.plugin.core.data.PackageSearchModule
import com.jetbrains.packagesearch.plugin.services.ModulesState
import com.jetbrains.packagesearch.plugin.ui.bridge.asTree
import org.jetbrains.jewel.IndeterminateHorizontalProgressBar
import org.jetbrains.jewel.bridge.toComposeColor
import org.jetbrains.jewel.foundation.tree.rememberTreeState
import org.jetbrains.jewel.themes.intui.standalone.IntUiTheme

@Composable
fun PackageSearchToolwindow(isInfoBoxOpen: Boolean) {

    val backgroundColor by remember(IntUiTheme.isDark) { mutableStateOf(JBColor.PanelBackground.toComposeColor()) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(top = 2.dp, bottom = 0.dp, start = 2.dp, end = 2.dp),
    ) {
        val modulesState by LocalProjectService.current.moduleData.collectAsState()
        when (val moduleProvider = modulesState) {
            is ModulesState.Loading -> IndeterminateHorizontalProgressBar(Modifier.fillMaxWidth())
            is ModulesState.Ready -> {
                LocalIsActionPerformingState.current.value = ActionState(false)
                val treeState = rememberTreeState()
                val (tree, nodesToOpen) =
                    moduleProvider.moduleData.asTree()
                var knownNodes: Set<PackageSearchModule.Identity> by remember { mutableStateOf(emptySet()) }
                remember(nodesToOpen) {
                    val result = nodesToOpen - knownNodes
                    treeState.openNodes(result.toList())
                    knownNodes = result
                }

                PackageSearchPackagePanel(
                    isInfoBoxOpen = isInfoBoxOpen,
                    tree = tree,
                    state = treeState,
                )
            }
        }
    }
}