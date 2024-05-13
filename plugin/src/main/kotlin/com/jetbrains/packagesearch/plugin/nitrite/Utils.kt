package com.jetbrains.packagesearch.plugin.nitrite

import kotlin.reflect.KProperty
import kotlinx.serialization.SerialName

operator fun KProperty<*>.div(other: KProperty<*>) =
    DocumentPathBuilder().append(this@div).append(other)

operator fun KProperty<*>.div(other: String) =
    DocumentPathBuilder().append(this@div).append(other)

operator fun String.div(other: KProperty<*>) =
    DocumentPathBuilder().append(this@div).append(other)

internal fun KProperty<*>.getSerializableName() =
    annotations.filterIsInstance<SerialName>()
        .firstOrNull()
        ?.value
        ?: name
