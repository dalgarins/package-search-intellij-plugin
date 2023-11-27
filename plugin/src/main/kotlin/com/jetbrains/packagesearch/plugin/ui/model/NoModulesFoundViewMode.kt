package com.jetbrains.packagesearch.plugin.ui.model

import com.intellij.dependencytoolwindow.extensionsFlow
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.jetbrains.packagesearch.plugin.PackageSearch
import com.jetbrains.packagesearch.plugin.core.utils.availableExtensionsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class NoModulesFoundViewMode(
    private val project: Project,
    private val viewModelScope: CoroutineScope,
) : Disposable {

    // for 232 compatibility
    constructor(project: Project) : this(project, CoroutineScope(SupervisorJob()))

    val isRefreshing = project.isProjectSyncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    
    val hasExternalProjects = ExternalSystemManager.EP_NAME
        .availableExtensionsFlow
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ExternalSystemManager.EP_NAME.extensions.isNotEmpty()
        )
    
    fun refreshExternalProjects() {
        viewModelScope.launch(Dispatchers.Main) {
            ExternalSystemManager.EP_NAME.extensions
                .map { ImportSpecBuilder(project, it.systemId) }
                .forEach { ExternalSystemUtil.refreshProjects(it) }
        }
    }

    override fun dispose() {
        if ("232" in PackageSearch.intelliJVersion) {
            viewModelScope.cancel()
        }
    }
    
}