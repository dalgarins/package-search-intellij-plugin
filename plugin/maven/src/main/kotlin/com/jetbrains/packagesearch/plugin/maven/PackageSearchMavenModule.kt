@file:Suppress("UnstableApiUsage")

package com.jetbrains.packagesearch.plugin.maven

import com.jetbrains.packagesearch.plugin.core.data.IconProvider.Icons
import com.jetbrains.packagesearch.plugin.core.data.InstallPackageData
import com.jetbrains.packagesearch.plugin.core.data.PackageSearchModule
import com.jetbrains.packagesearch.plugin.core.data.RemovePackageData
import com.jetbrains.packagesearch.plugin.core.data.UpdatePackageData
import com.jetbrains.packagesearch.plugin.core.utils.asMavenApiPackage
import java.nio.file.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.packagesearch.api.v3.ApiMavenPackage
import org.jetbrains.packagesearch.api.v3.ApiPackage
import org.jetbrains.packagesearch.api.v3.ApiPackageVersion
import org.jetbrains.packagesearch.api.v3.ApiRepository
import org.jetbrains.packagesearch.api.v3.search.PackagesType

data class MavenUpdatePackageData(
    override val installedPackage: PackageSearchDeclaredBaseMavenPackage,
    override val newVersion: String?,
    override val newScope: String?
) : UpdatePackageData

data class MavenInstallPackageData(
    override val apiPackage: ApiMavenPackage,
    override val selectedVersion: ApiPackageVersion,
    val selectedScope: String? = null
) : InstallPackageData

data class MavenRemovePackageData(
    override val declaredPackage: PackageSearchDeclaredBaseMavenPackage
) : RemovePackageData

@Serializable
@SerialName("maven")
data class PackageSearchMavenModule(
    override val name: String,
    override val identity: PackageSearchModule.Identity,
    override val buildFilePath: Path?,
    override val declaredKnownRepositories: Map<String, ApiRepository>,
    override val declaredDependencies: List<PackageSearchDeclaredBaseMavenPackage>,
    override val defaultScope: String? = null,
    override val availableScopes: List<String>,
    override val compatiblePackageTypes: List<PackagesType>,
) : PackageSearchModule.Base {

    override val dependencyMustHaveAScope: Boolean
        get() = false

    override val icon
        get() = Icons.MAVEN


    override fun getInstallData(
        apiPackage: ApiPackage,
        selectedVersion: ApiPackageVersion,
        selectedScope: String?
    ) = MavenInstallPackageData(
        apiPackage = apiPackage.asMavenApiPackage(),
        selectedVersion = selectedVersion,
        selectedScope = selectedScope
    )
}