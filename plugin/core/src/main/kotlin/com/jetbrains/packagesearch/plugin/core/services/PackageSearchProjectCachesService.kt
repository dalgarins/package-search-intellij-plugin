package com.jetbrains.packagesearch.plugin.core.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.jetbrains.packagesearch.plugin.core.PackageSearch
import com.jetbrains.packagesearch.plugin.core.utils.PKGSInternalAPI
import com.jetbrains.packagesearch.plugin.core.utils.packageSearchProjectDataPath
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import org.dizitart.kno2.nitrite
import org.dizitart.no2.Nitrite
import org.h2.mvstore.MVStore

@Service(Level.PROJECT)
class PackageSearchProjectCachesService(private val project: Project) : Disposable {

    private val cacheFilePath
        get() = project.packageSearchProjectDataPath / "caches-v${PackageSearch.databaseVersion}.db"


    @PKGSInternalAPI
    val cache = nitrite {
        loadModule(MVStoreModule.withConfig()
            .filePath(cacheFilePath.absolutePathString())
            .build())
    }

    @PKGSInternalAPI
    val cache = buildDefaultNitrate(cacheFilePath.absolutePathString())

    override fun dispose() = cache.close()

    inline fun <reified T : Any> getRepository(key: String) =
        cache.getRepository<T>(key)

}


fun buildDefaultNitrate(
    path: String,
    nitriteMapperConf: NitriteDocumentFormatBuilder.() -> Unit = {},
) = Nitrite.builder()
    .kotlinxNitriteMapper(builderAction = nitriteMapperConf)
    .filePath(path)
    .compressed()
    .openOrCreate()
    .asCoroutine()
