package org.jetbrains.packagesearch.plugin.gradle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.packagesearch.api.v3.ApiPackage
import org.jetbrains.packagesearch.packageversionutils.normalization.NormalizedVersion
import org.jetbrains.packagesearch.plugin.core.data.PackageSearchDeclaredMavenPackage
import org.jetbrains.packagesearch.plugin.core.data.PackageSearchDeclaredPackage
import org.jetbrains.packagesearch.plugin.core.data.WithIcon.PathSourceType
import org.jetbrains.packagesearch.plugin.core.extensions.DependencyDeclarationIndexes

@Serializable
@SerialName("gradle-version")
data class PackageSearchGradleDeclaredPackage(
    override val id: String,
    override val declaredVersion: NormalizedVersion,
    override val latestStableVersion: NormalizedVersion,
    override val latestVersion: NormalizedVersion,
    override val remoteInfo: ApiPackage?,
    override val declarationIndexes: DependencyDeclarationIndexes?,
    override val icon: PathSourceType,
    val module: String,
    val name: String,
    val configuration: String
) : PackageSearchDeclaredMavenPackage {
    override val groupId: String
        get() = module
    override val artifactId: String
        get() = name
    override val scope: String
        get() = configuration
}