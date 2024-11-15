@file:Suppress("UnstableApiUsage")

package com.jetbrains.packagesearch.plugin.gradle

import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.Module
import com.jetbrains.packagesearch.plugin.core.data.EditModuleContext
import com.jetbrains.packagesearch.plugin.core.data.IconProvider.Icons
import com.jetbrains.packagesearch.plugin.core.data.PackageSearchDeclaredPackage
import com.jetbrains.packagesearch.plugin.core.data.PackageSearchDeclaredRepository
import com.jetbrains.packagesearch.plugin.core.data.PackageSearchModule
import com.jetbrains.packagesearch.plugin.core.utils.toUnifiedDependency
import com.jetbrains.packagesearch.plugin.core.utils.toUnifiedRepository
import com.jetbrains.packagesearch.plugin.core.utils.validateMavenDeclaredPackageType
import com.jetbrains.packagesearch.plugin.core.utils.validateMavenPackageType
import com.jetbrains.packagesearch.plugin.core.utils.validateRepositoryType
import com.jetbrains.packagesearch.plugin.gradle.tooling.PackageSearchGradleJavaModel
import com.jetbrains.packagesearch.plugin.gradle.utils.toUnifiedRepository
import com.jetbrains.packagesearch.plugin.gradle.utils.validateRepositoryType
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.writeText
import org.jetbrains.packagesearch.api.v3.ApiPackage
import org.jetbrains.packagesearch.api.v3.ApiRepository
import org.jetbrains.packagesearch.api.v3.search.PackagesType

data class PackageSearchGradleModule(
    override val name: String,
    override val identity: PackageSearchModule.Identity,
    override val buildFilePath: Path?,
    override val declaredRepositories: List<PackageSearchGradleDeclaredRepository>,
    override val declaredDependencies: List<PackageSearchGradleDeclaredPackage>,
    override val defaultScope: String?,
    override val availableScopes: List<String>,
    override val compatiblePackageTypes: List<PackagesType>,
    val packageSearchModel: PackageSearchGradleJavaModel,
    val availableKnownRepositories: Map<String, ApiRepository>,
    val nativeModule: Module,
) : PackageSearchModule.Base {

    override val dependencyMustHaveAScope: Boolean
        get() = true

    override val icon
        get() = Icons.GRADLE

    val defaultConfiguration
        get() = defaultScope

    override suspend fun editModule(action: EditModuleContext.() -> Unit) {
        writeAction {
            val modifier = DependencyModifierService.getInstance(nativeModule.project)
            val editContext = EditGradleModuleContext(modifier)
            action(editContext)
        }
    }

    override fun updateDependency(
        context: EditModuleContext,
        declaredPackage: PackageSearchDeclaredPackage,
        newVersion: String?,
        newScope: String?,
    ) {
        validateMavenDeclaredPackageType(declaredPackage)
        val oldDescriptor = declaredPackage.toUnifiedDependency()
        val newDescriptor = oldDescriptor.copy(
            coordinates = oldDescriptor.coordinates
                .copy(version = newVersion ?: oldDescriptor.coordinates.version),
            scope = newScope ?: oldDescriptor.scope,
        )
        context.modifier.updateDependency(
            module = nativeModule,
            oldDescriptor = oldDescriptor,
            newDescriptor = newDescriptor,
        )
    }

    override fun addDependency(
        context: EditModuleContext,
        apiPackage: ApiPackage,
        selectedVersion: String,
        selectedScope: String?,
    ) {
        validateMavenPackageType(apiPackage)

        if (buildFilePath == null || !buildFilePath.exists()) {
            val isKotlin = buildFilePath?.extension?.equals("kts", ignoreCase = true) == true
            buildFilePath?.createParentDirectories()
                ?.writeText(buildString {
                    appendLine("dependencies {")
                    if (isKotlin) {
                        appendLine("    $selectedScope(\"${apiPackage.groupId}:${apiPackage.artifactId}:${selectedVersion}\")")
                    } else {
                        appendLine("    $selectedScope '${apiPackage.groupId}:${apiPackage.artifactId}:${selectedVersion}'")
                    }
                    appendLine("}")
                })
            return
        }

        context.modifier.addDependency(
            module = nativeModule,
            descriptor = UnifiedDependency(
                groupId = apiPackage.groupId,
                artifactId = apiPackage.artifactId,
                version = selectedVersion,
                configuration = selectedScope,
            )
        )
    }

    override fun removeDependency(
        context: EditModuleContext,
        declaredPackage: PackageSearchDeclaredPackage,
    ) {
        validateMavenDeclaredPackageType(declaredPackage)
        context.modifier.removeDependency(
            module = nativeModule,
            descriptor = declaredPackage.toUnifiedDependency(),
        )
    }

    override fun addRepository(
        context: EditModuleContext,
        repository: ApiRepository
    ) {
        validateRepositoryType(repository)
        context.modifier.addRepository(
            module = nativeModule,
            repository = repository.toUnifiedRepository(),
        )
    }

    override fun removeRepository(
        context: EditModuleContext,
        repository: PackageSearchDeclaredRepository,
    ) {
        validateRepositoryType(repository)
        context.modifier.deleteRepository(
            module = nativeModule,
            repository = repository.toUnifiedRepository(),
        )
    }
}
