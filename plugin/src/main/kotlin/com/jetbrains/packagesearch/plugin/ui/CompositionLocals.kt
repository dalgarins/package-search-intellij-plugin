package com.jetbrains.packagesearch.plugin.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.Application
import com.jetbrains.packagesearch.plugin.core.utils.flow
import com.jetbrains.packagesearch.plugin.services.PackageSearchProjectService
import com.jetbrains.packagesearch.plugin.ui.bridge.isLightTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import org.jetbrains.packagesearch.api.v3.http.PackageSearchApi

val LocalJson = staticCompositionLocalOf { Json { prettyPrint = true } }

val LocalPackageSearchService = staticCompositionLocalOf<PackageSearchProjectService> {
    error("No ProjectService provided")
}

val LocalProjectCoroutineScope = staticCompositionLocalOf<CoroutineScope> {
    error("No ProjectCoroutineScope provided")
}

val LocalIsActionPerformingState: ProvidableCompositionLocal<MutableState<ActionState?>> =
    staticCompositionLocalOf { error("No LocalIsActionPerformingState provided") }


val LocalInfoBoxPanelOpenState: ProvidableCompositionLocal<MutableState<Boolean>> =
    staticCompositionLocalOf { error("No LocalInfoBoxPanelOpenState provided") }

val LocalPackageSearchApiClient: ProvidableCompositionLocal<PackageSearchApi> =
    staticCompositionLocalOf { error("No LocalPackageSearchApiClient provided") }

val LocalIsOnlyStableVersions: ProvidableCompositionLocal<MutableStateFlow<Boolean>> =
    staticCompositionLocalOf { error("No LocalIsOnlyStableVersions provided") }

val LocalGlobalPopupIdState: ProvidableCompositionLocal<MutableState<String?>> =
    staticCompositionLocalOf { error("No LocalDropDownItemId provided") }

fun Application.lightThemeFlow() =
    messageBus.flow(LafManagerListener.TOPIC) { LafManagerListener { trySend(isLightTheme()) } }
