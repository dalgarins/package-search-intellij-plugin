package com.jetbrains.packagesearch.plugin.ui.model.infopanel

import com.jetbrains.packagesearch.plugin.PackageSearchBundle.message
import com.jetbrains.packagesearch.plugin.core.extensions.PackageSearchKnownRepositoriesContext
import com.jetbrains.packagesearch.plugin.core.utils.icon
import com.jetbrains.packagesearch.plugin.ui.model.getLatestVersion
import org.jetbrains.packagesearch.api.v3.ApiMavenPackage
import org.jetbrains.packagesearch.api.v3.ApiPackage

context(PackageSearchKnownRepositoriesContext)
fun ApiPackage.repositories() =
    versions.latest
        .repositoryIds
        .mapNotNull { knownRepositories[it] }
        .map { InfoPanelContent.PackageInfo.Repository(it.name, it.url) }

context(PackageSearchKnownRepositoriesContext)
internal fun InfoPanelContentEvent.Package.Declared.Base.asPanelContent(
    onlyStable: Boolean,
    isLoading: Boolean,
) =
    listOf(
        InfoPanelContent.PackageInfo.Declared.Base(
            moduleId = module.identity,
            packageListId = packageListId,
            tabTitle = message("packagesearch.ui.toolwindow.packages.details.info.overview"),
            title = declaredPackage.displayName,
            subtitle = declaredPackage.coordinates,
            icon = declaredPackage.icon,
            type = when (declaredPackage.remoteInfo) {
                is ApiMavenPackage -> message("packagesearch.configuration.maven.title")
                null -> message("packagesearch.ui.toolwindow.packages.details.info.unknown")
            },
            licenses = declaredPackage.remoteInfo?.licenses?.asInfoPanelLicenseList() ?: emptyList(),
            authors = declaredPackage.remoteInfo?.authors?.mapNotNull { it.name } ?: emptyList(),
            description = declaredPackage.remoteInfo
                ?.description
                ?.sanitizeDescription(),
            scm = declaredPackage.remoteInfo?.scm?.asInfoPanelScm(),
            readmeUrl = declaredPackage.remoteInfo?.scm?.readmeUrl,
            repositories = declaredPackage.remoteInfo?.repositories() ?: emptyList(),
            latestVersion = declaredPackage.getLatestVersion(onlyStable)?.versionName,
            declaredVersion = declaredPackage.declaredVersion
                ?.versionName
                ?: message("packagesearch.ui.missingVersion"),
            declaredScope = declaredPackage.declaredScope
                ?: message("packagesearch.ui.missingScope"),
            availableVersions = declaredPackage.remoteInfo
                ?.versions
                ?.all
                ?.filter { if (onlyStable) it.normalizedVersion.isStable else true }
                ?.map { it.normalizedVersion.versionName }
                ?: emptyList(),
            availableScopes = module.availableScopes,
            isLoading = isLoading,
            allowMissingScope = !module.dependencyMustHaveAScope
        )
    )

private fun String.sanitizeDescription() =
    replace("\r\n", "\n")
        .replace("\r", "\n")
        .split("\n")
        .joinToString("\n") { it.trimStart() }


context(PackageSearchKnownRepositoriesContext)
internal fun InfoPanelContentEvent.Package.Declared.WithVariant.asPanelContent(
    onlyStable: Boolean,
    isLoading: Boolean,
) =
    listOf(
        InfoPanelContent.PackageInfo.Declared.WithVariant(
            moduleId = module.identity,
            packageListId = packageListId,
            tabTitle = message("packagesearch.ui.toolwindow.packages.details.info.overview"),
            title = declaredPackage.displayName,
            subtitle = declaredPackage.coordinates,
            icon = declaredPackage.icon,
            type = when (declaredPackage.remoteInfo) {
                is ApiMavenPackage -> message("packagesearch.configuration.maven.title")
                null -> message("packagesearch.ui.toolwindow.packages.details.info.unknown")
            },
            licenses = declaredPackage.remoteInfo?.licenses?.asInfoPanelLicenseList() ?: emptyList(),
            authors = declaredPackage.remoteInfo?.authors?.mapNotNull { it.name } ?: emptyList(),
            description = declaredPackage.remoteInfo
                ?.description
                ?.sanitizeDescription(),
            scm = declaredPackage.remoteInfo?.scm?.asInfoPanelScm(),
            readmeUrl = declaredPackage.remoteInfo?.scm?.readmeUrl,
            repositories = declaredPackage.remoteInfo?.repositories() ?: emptyList(),
            latestVersion = declaredPackage.getLatestVersion(onlyStable)?.versionName,
            declaredVersion = declaredPackage.declaredVersion
                ?.versionName
                ?: message("packagesearch.ui.missingVersion"),
            declaredScope = declaredPackage.declaredScope
                ?: message("packagesearch.ui.missingScope"),
            availableVersions = declaredPackage.remoteInfo
                ?.versions
                ?.all
                ?.filter { if (onlyStable) it.normalizedVersion.isStable else true }
                ?.map { it.normalizedVersion.versionName }
                ?: emptyList(),
            availableScopes = module.variants.getValue(variantName).availableScopes,
            isLoading = isLoading,
            compatibleVariants = module.variants.keys.sorted() - variantName,
            declaredVariant = variantName,
            allowMissingScope = !module.dependencyMustHaveAScope,
            variantTerminology = module.variantTerminology
        )
    )

context(PackageSearchKnownRepositoriesContext)
internal fun InfoPanelContentEvent.Package.Remote.WithVariants.asPanelContent(
    isLoading: Boolean,
) =
    listOf(
        InfoPanelContent.PackageInfo.Remote.WithVariant(
            tabTitle = message("packagesearch.ui.toolwindow.packages.details.info.overview"),
            moduleId = module.identity,
            packageListId = packageListId,
            title = apiPackage.name,
            subtitle = apiPackage.coordinates,
            icon = apiPackage.icon,
            type = when (apiPackage) {
                is ApiMavenPackage -> message("packagesearch.configuration.maven.title")
            },
            licenses = apiPackage.licenses?.asInfoPanelLicenseList() ?: emptyList(),
            authors = apiPackage.authors.mapNotNull { it.name },
            description = apiPackage.description?.sanitizeDescription(),
            scm = apiPackage.scm?.asInfoPanelScm(),
            readmeUrl = apiPackage.scm?.readmeUrl,
            primaryVariant = primaryVariantName,
            additionalVariants = compatibleVariantNames.sorted() - primaryVariantName,
            repositories = apiPackage.repositories(),
            isLoading = isLoading,
            isInstalledInPrimaryVariant = module.variants.getValue(primaryVariantName).declaredDependencies
                .any { it.id == apiPackage.id }
        )
    )

context(PackageSearchKnownRepositoriesContext)
internal fun InfoPanelContentEvent.Package.Remote.Base.asPanelContent(
    isLoading: Boolean,
) = listOf(
    InfoPanelContent.PackageInfo.Remote.Base(
        tabTitle = message("packagesearch.ui.toolwindow.packages.details.info.overview"),
        moduleId = module.identity,
        packageListId = packageListId,
        title = apiPackage.name,
        subtitle = apiPackage.coordinates,
        icon = apiPackage.icon,
        type = when (apiPackage) {
            is ApiMavenPackage -> message("packagesearch.configuration.maven.title")
        },
        licenses = apiPackage.licenses?.asInfoPanelLicenseList() ?: emptyList(),
        authors = apiPackage.authors.mapNotNull { it.name },
        description = apiPackage.description?.sanitizeDescription(),
        scm = apiPackage.scm?.asInfoPanelScm(),
        readmeUrl = apiPackage.scm?.readmeUrl,
        repositories = apiPackage.repositories(),
        isLoading = isLoading
    )
)