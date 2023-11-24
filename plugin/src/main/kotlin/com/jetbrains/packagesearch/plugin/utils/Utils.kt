@file:Suppress("UnstableApiUsage")

package com.jetbrains.packagesearch.plugin.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.util.Function
import com.jetbrains.packagesearch.plugin.core.utils.FlowWithInitialValue
import com.jetbrains.packagesearch.plugin.core.utils.flow
import com.jetbrains.packagesearch.plugin.core.utils.withInitialValue
import io.ktor.client.plugins.logging.Logger
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import org.jetbrains.packagesearch.api.v3.search.SearchParametersBuilder
import org.jetbrains.packagesearch.api.v3.search.buildSearchParameters

// keep the type explicit, otherwise the Kotlin plugin breaks
internal val Project.nativeModules: List<NativeModule>
    get() = ModuleManager.getInstance(this).modules.toList()

internal val Project.nativeModulesFlow: FlowWithInitialValue<List<NativeModule>>
    get() = messageBus.flow(ModuleListener.TOPIC) {
        object : ModuleListener {
            override fun modulesAdded(project: Project, modules: NativeModules) {
                trySend(nativeModules)
            }

            override fun moduleRemoved(project: Project, module: Module) {
                trySend(nativeModules)
            }

            override fun modulesRenamed(
                project: Project,
                modules: MutableList<out Module>,
                oldNameProvider: Function<in Module, String>,
            ) {
                trySend(nativeModules)
            }
        }
    }.withInitialValue(nativeModules)

fun interval(
    interval: Duration,
    emitOnStart: Boolean = false,
) = flow {
    if (emitOnStart) {
        emit(Unit)
    }
    while (true) {
        delay(interval)
        emit(Unit)
    }
}

typealias NativeModules = List<Module>
typealias NativeModule = Module

fun <T> Flow<T?>.startWithNull() = onStart { emit(null) }

@Suppress("FunctionName")
fun KtorDebugLogger() = object : Logger {
    override fun log(message: String) = logDebug(message)
}

@Composable
fun <T> FlowWithInitialValue<T>.collectAsState() =
    collectAsState(initialValue)

suspend fun PackageSearchApiPackageCache.searchPackages(builder: SearchParametersBuilder.() -> Unit) =
    searchPackages(buildSearchParameters(builder))

/**
 * Returns a [Flow] whose values are generated by [transform] function that process the most recently emitted values by each flow.
 *
 * The receiver of the [transform] is [FlowCollector] and thus `transform` is a
 * generic function that may transform emitted element, skip it or emit it multiple times.
 */
@Suppress("UNCHECKED_CAST")
fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<Any> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7,
    )
}

@Suppress("UNCHECKED_CAST")
internal fun <K, V> Map<K?, V>.filterNotNullKeys() =
    filterKeys { it != null } as Map<K, V>

internal fun <T> timer(interval: Duration, generate: suspend () -> T) = flow {
    while (true) {
        emit(generate())
        delay(interval)
    }
}