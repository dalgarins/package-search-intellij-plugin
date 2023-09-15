package com.jetbrains.packagesearch.plugin.core.nitrite.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.dizitart.no2.FindOptions
import org.dizitart.no2.IndexOptions
import org.dizitart.no2.RemoveOptions
import org.dizitart.no2.WriteResult
import org.dizitart.no2.event.ChangeListener
import org.dizitart.no2.event.ChangeType
import org.dizitart.no2.objects.ObjectFilter
import org.dizitart.no2.objects.ObjectRepository
import org.dizitart.no2.objects.filters.ObjectFilters
import com.jetbrains.packagesearch.plugin.core.nitrite.DocumentPathBuilder
import com.jetbrains.packagesearch.plugin.core.nitrite.asKotlin
import com.jetbrains.packagesearch.plugin.core.nitrite.serialization.NitriteDocumentFormat
import com.jetbrains.packagesearch.plugin.core.utils.PKGSInternalAPI
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import org.dizitart.no2.NitriteId

class CoroutineObjectRepository<T : Any> @PKGSInternalAPI constructor(
    val synchronous: ObjectRepository<T>,
    val type: KType,
    private val documentFormat: NitriteDocumentFormat,
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CoroutineWrapper() {

    data class Change<T>(val changeType: ChangeType, val changedItems: Flow<T>)
    data class Item<T>(val changeTimestamp: Instant, val changeType: ChangeType, val item: T)

    val documentCollection
        get() = CoroutineNitriteCollection(synchronous.documentCollection)

    private val indexMutex = Mutex()

    suspend fun findById(id: Long): T? = dispatch { synchronous.getById(NitriteId.createId(id)) }
    suspend fun findById(id: Int): T? = findById(id.toLong())

    suspend fun find(filter: ObjectFilter? = null, findOptions: FindOptions? = null): Flow<T> =
        withContext(dispatcher) {
            when {
                filter != null && findOptions != null -> synchronous.find(filter, findOptions).asFlow()
                filter != null -> synchronous.find(filter).asFlow()
                else -> synchronous.find().asFlow()
            }.flowOn(dispatcher)
        }

    fun changes(): Flow<Change<Item<T>>> = channelFlow {
        val listener = ChangeListener { trySend(it.asKotlin(documentFormat, type)) }
        synchronous.register(listener)
        awaitClose { synchronous.deregister(listener) }
    }

    suspend fun insert(items: Array<T>): WriteResult =
        dispatch { synchronous.insert(items) }

    suspend fun update(filter: ObjectFilter, update: T, upsert: Boolean = false) =
        dispatch { synchronous.update(filter, update, upsert) }

    suspend fun removeAll() = dispatch { synchronous.remove(ObjectFilters.ALL) }

    suspend fun remove(filter: ObjectFilter, removeOptions: RemoveOptions? = null): WriteResult =
        dispatch {
            if (removeOptions != null) synchronous.remove(filter, removeOptions)
            else synchronous.remove(filter)
        }

    suspend fun remove(element: T): WriteResult =
        dispatch { synchronous.remove(element) }

    suspend fun createIndex(
        indexOptions: IndexOptions,
        path: DocumentPathBuilder,
    ) = indexMutex.withLock {
        val field = path.build()
        dispatch { if (!synchronous.hasIndex(field)) synchronous.createIndex(field, indexOptions) }
    }

    suspend fun createIndex(
        indexOptions: IndexOptions,
        path: KProperty<*>,
    ) = indexMutex.withLock {
        dispatch { if (!synchronous.hasIndex(path.name)) synchronous.createIndex(path.name, indexOptions) }
    }

}