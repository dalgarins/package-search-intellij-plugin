package com.jetbrains.packagesearch.plugin.nitrite

import com.jetbrains.packagesearch.plugin.core.data
import kotlin.reflect.KProperty
import org.dizitart.no2.filters.NitriteFilter

object NitriteFilters {

    object Object {
        fun eq(path: DocumentPathBuilder, value: Any): NitriteFilter =
            .eq(path.build(), value)

        fun <T> eq(path: KProperty<T>, value: T): ObjectFilter =
            ObjectFilters.eq(path.name, value)

        fun `in`(path: DocumentPathBuilder, value: Array<Any>): ObjectFilter =
            ObjectFilters.`in`(path.build(), *value)

        fun `in`(path: DocumentPathBuilder, value: Collection<Any>): ObjectFilter =
            `in`(path, value.toTypedArray())

        fun <T> `in`(path: KProperty<T>, value: Collection<Any>): ObjectFilter =
            ObjectFilters.`in`(path.name, *value.toTypedArray())

        fun `in`(path: String, value: Collection<Any>): ObjectFilter =
            ObjectFilters.`in`(path, *value.toTypedArray())

        val ALL
            get() = NitriteFilter.ALL
    }
}