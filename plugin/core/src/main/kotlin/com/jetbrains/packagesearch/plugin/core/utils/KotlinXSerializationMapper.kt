package com.jetbrains.packagesearch.plugin.core.utils

import org.dizitart.no2.NitriteConfig
import org.dizitart.no2.collection.Document
import org.dizitart.no2.collection.NitriteId
import org.dizitart.no2.common.mapper.NitriteMapper
import org.dizitart.no2.exceptions.ObjectMappingException

class KotlinXSerializationMapper : NitriteMapper {
    private fun <Target : Any> convertFromDocument(source: Document?, type: Class<Target>): Target? =
        source?.let { DocumentDecoder.decodeFromDocument(source, type) }

    private fun <Source : Any> convertToDocument(source: Source): Document = DocumentEncoder.encodeToDocument(source)

    override fun <Source, Target : Any> tryConvert(source: Source, type: Class<Target>): Any? =
        when (source) {
            is Document -> convertFromDocument(source, type)
            else -> source
        }

    private fun isValueType(type: Class<*>): Boolean {
        if (type.isPrimitive && type != Void.TYPE) return true
        if (valueTypes.contains(type)) return true
        return valueTypes.any { it.isAssignableFrom(type) }
    }

    private val valueTypes: List<Class<*>> = listOf(
        Number::class.java,
        Boolean::class.java,
        Character::class.java,
        String::class.java,
        Array<Byte>::class.java,
        Enum::class.java,
        NitriteId::class.java,
    )

    override fun initialize(nitriteConfig: NitriteConfig) {}
}