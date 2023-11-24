package com.jetbrains.packagesearch.plugin.ui.panels.packages

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import com.jetbrains.packagesearch.plugin.PackageSearchBundle.message
import com.jetbrains.packagesearch.plugin.core.data.IconProvider
import com.jetbrains.packagesearch.plugin.ui.LearnMoreLink
import com.jetbrains.packagesearch.plugin.ui.PackageSearchMetrics
import com.jetbrains.packagesearch.plugin.ui.bridge.LabelInfo
import com.jetbrains.packagesearch.plugin.ui.bridge.PackageActionPopup
import com.jetbrains.packagesearch.plugin.ui.bridge.TextSelectionDropdown
import com.jetbrains.packagesearch.plugin.ui.model.packageslist.PackageListItem
import com.jetbrains.packagesearch.plugin.ui.model.packageslist.PackageListItemEvent
import com.jetbrains.packagesearch.plugin.ui.model.packageslist.PackageListItemEvent.EditPackageEvent.SetPackageScope
import com.jetbrains.packagesearch.plugin.ui.model.packageslist.PackageListItemEvent.EditPackageEvent.SetPackageVersion
import com.jetbrains.packagesearch.plugin.ui.model.packageslist.PackageListItemEvent.InfoPanelEvent.OnPackageDoubleClick
import com.jetbrains.packagesearch.plugin.ui.model.packageslist.PackageListItemEvent.InfoPanelEvent.OnPackageSelected
import com.jetbrains.packagesearch.plugin.ui.model.packageslist.PackageListItemEvent.OnPackageAction.GoToSource
import com.jetbrains.packagesearch.plugin.ui.model.packageslist.PackageListItemEvent.OnPackageAction.Install
import com.jetbrains.packagesearch.plugin.ui.model.packageslist.PackageListItemEvent.OnPackageAction.Remove
import com.jetbrains.packagesearch.plugin.ui.panels.packages.items.PackageListItem
import org.jetbrains.jewel.foundation.lazy.SelectableLazyColumn
import org.jetbrains.jewel.foundation.lazy.SelectableLazyItemScope
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListState
import org.jetbrains.jewel.foundation.lazy.SelectionMode
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import org.jetbrains.jewel.ui.component.styling.LocalLazyTreeStyle

@Composable
fun PackageSearchPackageList(
    packagesList: List<PackageListItem>,
    isCompact: Boolean,
    selectableLazyListState: SelectableLazyListState,
    onPackageEvent: (PackageListItemEvent) -> Unit,
) {
    var openPopupId by remember { mutableStateOf<PackageListItem.Package.Id?>(null) }
    SelectableLazyColumn(
        selectionMode = SelectionMode.Single,
        state = selectableLazyListState,
        onSelectedIndexesChanged = {
            val eventId = it.firstOrNull()
                ?.let { packagesList[it] }
                ?.id as? PackageListItem.Package.Id
            if (eventId != null) {
                onPackageEvent(OnPackageSelected(eventId))
            }
        },
    ) {
        packagesList.forEachIndexed { index, item ->
            when (item) {
                is PackageListItem.Header -> stickyHeader(item.id, "header") {
                    PackageListItem(item, onPackageEvent)
                }

                is PackageListItem.Package -> item(item.id, contentType = item.contentType()) {
                    PackageListItem(
                        content = item,
                        packagesList = packagesList,
                        index = index,
                        onPackageListItemEvent = onPackageEvent,
                        isCompact = isCompact,
                        openPopupId = openPopupId,
                        onOpenPopupRequest = { openPopupId = it },
                        onPopupDismissRequest = { openPopupId = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableLazyItemScope.PackageListItem(
    content: PackageListItem.Package,
    packagesList: List<PackageListItem>,
    index: Int,
    onPackageListItemEvent: (PackageListItemEvent) -> Unit,
    isCompact: Boolean,
    openPopupId: PackageListItem.Package.Id?,
    onOpenPopupRequest: (PackageListItem.Package.Id) -> Unit,
    onPopupDismissRequest: () -> Unit,
) {
    val itemPaddings = PackageSearchMetrics.PackagesList.Package.paddingFor(
        isFirstItem = packagesList.getOrNull(index - 1) is PackageListItem.Header,
        isLastItem = packagesList.getOrNull(index + 1) !is PackageListItem.Package
    )
    Box(
        modifier = Modifier
            .padding(itemPaddings)
            .onClick(
                interactionSource = remember { MutableInteractionSource() },
                onDoubleClick = { onPackageListItemEvent(OnPackageDoubleClick(content.id)) },
                onClick = { }
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PackageSearchMetrics.PackageList.Item.height)
                .background(
                    when {
                        isSelected && isActive -> LocalLazyTreeStyle.current.colors.elementBackgroundSelectedFocused
                        isSelected && !isActive -> LocalLazyTreeStyle.current.colors.elementBackgroundSelected
                        else -> Color.Transparent
                    },
                ).padding(start = PackageSearchMetrics.PackageList.Item.padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PackageTitle(content)

            if (content is PackageListItem.Package.Declared && !isCompact) {
                DeclaredPackageActions(content, onPackageListItemEvent)
            }

            PackageActions(content, onPackageListItemEvent, openPopupId, onOpenPopupRequest, onPopupDismissRequest)
        }
    }
}

@Composable
private fun RowScope.DeclaredPackageActions(
    item: PackageListItem.Package.Declared,
    onPackageListItemEvent: (PackageListItemEvent) -> Unit,
) {
    Row(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        ScopeSelectionDropdown(
            declaredScope = item.selectedScope,
            allowMissingScope = item.allowMissingScope,
            availableScopes = item.availableScopes,
            enabled = !item.isLoading,
            onScopeChanged = { onPackageListItemEvent(SetPackageScope(item.id, it)) }
        )

        VersionSelectionDropdown(
            declaredVersion = item.declaredVersion,
            availableVersions = item.availableVersions,
            latestVersion = item.latestVersion,
            enabled = !item.isLoading,
            onVersionChanged = { onPackageListItemEvent(SetPackageVersion(item.id, it)) }
        )
    }
}

@Composable
private fun PackageActions(
    item: PackageListItem.Package,
    onPackageListItemEvent: (PackageListItemEvent) -> Unit,
    openPopupId: PackageListItem.Package.Id?,
    onOpenPopupRequest: (PackageListItem.Package.Id) -> Unit,
    onPopupDismissRequest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .defaultMinSize(90.dp, 16.dp)
            .padding(start = 8.dp, end = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item is PackageListItem.Package.Declared && item.latestVersion != null) {
            Link(
                enabled = !item.isLoading,
                text = message("packagesearch.ui.toolwindow.packages.actions.update"),
                onClick = { onPackageListItemEvent(SetPackageVersion(item.id, item.latestVersion)) },
            )
        }

        val installEvent = when (item) {
            is PackageListItem.Package.Declared -> null
            is PackageListItem.Package.Remote.Base -> Install.Base(item.id, item.id.headerId)
            is PackageListItem.Package.Remote.WithVariant -> when {
                !item.isInstalledInPrimaryVariant -> {
                    Install.WithVariant(
                        eventId = item.id,
                        headerId = item.id.headerId,
                        selectedVariantName = item.primaryVariantName
                    )
                }
                else -> null
            }
        }

        if (installEvent != null) {
            Link(
                enabled = !item.isLoading,
                text = message("packagesearch.ui.toolwindow.packages.actions.install"),
                onClick = { onPackageListItemEvent(installEvent) },
            )
        }

        Box(modifier = Modifier.width(24.dp).padding(start = 8.dp)) {
            when {
                item.isLoading -> CircularProgressIndicator()
                else -> when (item) {
                    is PackageListItem.Package.Declared -> DeclaredPackageActionPopup(
                        isOpen = openPopupId == item.id,
                        onOpenPopupRequest = { onOpenPopupRequest(item.id) },
                        onDismissRequest = onPopupDismissRequest,
                        iconResource = "actions/more.svg",
                        onGoToSource = { onPackageListItemEvent(GoToSource(item.id)) },
                        onRemove = { onPackageListItemEvent(Remove(item.id)) },
                    )

                    is PackageListItem.Package.Remote.WithVariant ->
                        RemotePackageWithVariantsActionPopup(
                            isOpen = openPopupId == item.id,
                            primaryVariantName = item.primaryVariantName,
                            additionalVariants = item.additionalVariants,
                            isInstalledInPrimaryVariant = item.isInstalledInPrimaryVariant,
                            onOpenPopupRequest = { onOpenPopupRequest(item.id) },
                            onDismissRequest = onPopupDismissRequest,
                            onInstall = { onPackageListItemEvent(Install.WithVariant(item.id, item.id.headerId, it)) }
                        )

                    is PackageListItem.Package.Remote.Base -> {}
                }
            }
        }
    }
}

@Composable
private fun RowScope.PackageTitle(item: PackageListItem.Package) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.Companion.weight(2f).fillMaxWidth()
    ) {
        Icon(
            resource = if (JewelTheme.isDark) item.icon.darkIconPath else item.icon.lightIconPath,
            modifier = Modifier.size(16.dp),
            contentDescription = null,
            iconClass = IconProvider::class.java
        )
        Text(text = item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        LabelInfo(text = item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun RemotePackageWithVariantsActionPopup(
    isOpen: Boolean,
    primaryVariantName: String,
    additionalVariants: List<String>,
    isInstalledInPrimaryVariant: Boolean,
    onOpenPopupRequest: () -> Unit,
    onDismissRequest: () -> Unit,
    onInstall: (String) -> Unit,
) {
    if (additionalVariants.isEmpty() && isInstalledInPrimaryVariant) {
        return
    }
    PackageActionPopup(
        isOpen = isOpen,
        iconResource = "actions/more.svg",
        clazz = AllIcons::class.java,
        onOpenPopupRequest = onOpenPopupRequest,
        onDismissRequest = onDismissRequest
    ) {
        passiveItem {
            Text(text = message("packagesearch.ui.toolwindow.actions.addTo"), fontWeight = FontWeight.Bold)
        }
        if (!isInstalledInPrimaryVariant) {
            separator()
            selectableItem(
                selected = false,
                onClick = { onInstall(primaryVariantName) }
            ) {
                Text(text = primaryVariantName)
            }
        }

        if (additionalVariants.isNotEmpty()) {
            separator()
            additionalVariants.forEach {
                selectableItem(
                    selected = false,
                    onClick = { onInstall(it) }
                ) {
                    Text(text = it)
                }
            }
        }
    }

}

@Composable
internal fun DeclaredPackageActionPopup(
    isOpen: Boolean,
    onOpenPopupRequest: () -> Unit,
    onDismissRequest: () -> Unit,
    onGoToSource: () -> Unit,
    onRemove: () -> Unit,
    iconResource: String = "actions/more.svg",
) {
    PackageActionPopup(
        isOpen = isOpen,
        iconResource = iconResource,
        onOpenPopupRequest = onOpenPopupRequest,
        onDismissRequest = onDismissRequest,
        content = {
            selectableItem(
                selected = false,
                onClick = onGoToSource,
            ) {
                Text(text = message("packagesearch.ui.toolwindow.packages.actions.gotToSource"))
            }
            separator()
            selectableItem(
                selected = false,
                onClick = onRemove,
            ) {
                Text(text = message("packagesearch.ui.toolwindow.packages.actions.remove"))
            }
        }
    )
}

private fun PackageListItem.contentType() = when (this) {
    is PackageListItem.Header -> "header"
    is PackageListItem.Package.Declared -> "declared.package"
    is PackageListItem.Package.Remote -> "remote.package"
}

@Composable
fun NoResultsToShow(
    onLinkClick: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LabelInfo(message("packagesearch.ui.toolwindow.packages.empty.noSupportedDependencyFound.l1"))
        LabelInfo(message("packagesearch.ui.toolwindow.packages.empty.noSupportedDependencyFound.l2"))
        LearnMoreLink(onLinkClick)
    }
}

@Composable
fun VersionSelectionDropdown(
    declaredVersion: String?,
    availableVersions: List<String>,
    latestVersion: String?,
    enabled: Boolean,
    onVersionChanged: (String) -> Unit,
) {
    val contentText = buildString {
        when (declaredVersion) {
            null -> append(message("packagesearch.ui.missingVersion"))
            else -> {
                append(declaredVersion)
                if (latestVersion != null) {
                    append(" → ")
                    append(latestVersion)
                }
            }
        }
    }
    TextSelectionDropdown(
        items = availableVersions,
        content = contentText,
        enabled = enabled,
        onSelection = onVersionChanged,
    )
}

@Composable
fun ScopeSelectionDropdown(
    declaredScope: String?,
    availableScopes: List<String>,
    allowMissingScope: Boolean,
    enabled: Boolean,
    onScopeChanged: (String?) -> Unit,
) {
    TextSelectionDropdown(
        items = buildList {
            if (allowMissingScope) {
                add(message("packagesearch.ui.missingScope"))
            }
            addAll(availableScopes)
        },
        content = declaredScope ?: message("packagesearch.ui.missingScope"),
        enabled = enabled,
        onSelection = {
            if (it == message("packagesearch.ui.missingScope")) {
                onScopeChanged(null)
            } else {
                onScopeChanged(it)
            }
        },
    )
}
