/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.io.envelopes

import hep.dataforge.context.Global
import hep.dataforge.io.MetaStreamReader
import hep.dataforge.io.MetaStreamWriter
import hep.dataforge.io.envelopes.Envelope.Companion.META_TYPE_PROPERTY

/**
 *
 * @author Alexander Nozik
 */
interface MetaType {


    val codes: List<Short>

    val name: String

    val reader: MetaStreamReader

    val writer: MetaStreamWriter

    /**
     * A file name filter for meta encoded in this format
     * @return
     */
    val fileNameFilter: (String) -> Boolean

    companion object {

        /**
         * Lazy cache of meta types to improve performance
         */
        private val metaTypes by lazy{
            Global.serviceStream(MetaType::class.java).toList()
        }

        /**
         * Resolve a meta type code and return null if code could not be resolved
         * @param code
         * @return
         */
        fun resolve(code: Short): MetaType? {
            return metaTypes.firstOrNull { it -> it.codes.contains(code) }

        }

        /**
         * Resolve a meta type and return null if it could not be resolved
         * @param name
         * @return
         */
        fun resolve(name: String): MetaType? {
            return Global.serviceStream(MetaType::class.java)
                    .filter { it -> it.name.equals(name, ignoreCase = true) }.findFirst().orElse(null)
        }

        fun resolve(properties: Map<String, String>): MetaType {
            return properties[META_TYPE_PROPERTY]?.let { MetaType.resolve(it) } ?: xmlMetaType
        }
    }
}
