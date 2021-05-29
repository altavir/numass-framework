/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hep.dataforge.io.envelopes

import hep.dataforge.data.Data
import hep.dataforge.data.binary.Binary
import hep.dataforge.description.NodeDef
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import hep.dataforge.nullable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.Serializable
import java.time.Instant
import java.util.function.Function

/**
 * The message is a pack that can include two principal parts:
 *
 *  * Envelope meta-data
 *  * binary data
 *
 *
 * @author Alexander Nozik
 */
@NodeDef(key = "@envelope", info = "An optional envelope service info node")
interface Envelope : Metoid, Serializable {

    /**
     * Read data into buffer. This operation could take a lot of time so be
     * careful when performing it synchronously
     *
     * @return
     */
    val data: Binary

    /**
     * The purpose of the envelope
     *
     * @return
     */
    val type: String?
        get() = meta.optString(ENVELOPE_TYPE_KEY).nullable

    /**
     * The type of data encoding
     *
     * @return
     */
    val dataType: String?
        get() = meta.optString(ENVELOPE_DATA_TYPE_KEY).nullable

    /**
     * Textual user friendly description
     *
     * @return
     */
    val description: String?
        get() = meta.optString(ENVELOPE_DESCRIPTION_KEY).nullable

    /**
     * Time of creation of the envelope
     *
     * @return
     */
    val time: Instant?
        get() = meta.optTime(ENVELOPE_TIME_KEY).nullable

    /**
     * Meta part of the envelope
     *
     * @return
     */
    override val meta: Meta

    fun hasMeta(): Boolean {
        return !meta.isEmpty
    }

    fun hasData(): Boolean {
        return try {
            data.size > 0
        } catch (e: IOException) {
            LoggerFactory.getLogger(javaClass).error("Failed to estimate data size in the envelope", e)
            false
        }

    }

    /**
     * Transform Envelope to Lazy data using given transformation.
     * In case transformation failed an exception will be thrown in call site.
     *
     * @param type
     * @param transform
     * @param <T>
     * @return
    </T> */
    fun <T> map(type: Class<T>, transform: Function<Binary, T>): Data<T> {
        return Data.generate(type, meta) { transform.apply(data) }
    }

    companion object {
        /**
         * Property keys
         */
        const val TYPE_PROPERTY = "type"
        const val META_TYPE_PROPERTY = "metaType"
        const val META_LENGTH_PROPERTY = "metaLength"
        const val DATA_LENGTH_PROPERTY = "dataLength"

        /**
         * meta keys
         */
        const val ENVELOPE_NODE = "@envelope"
        const val ENVELOPE_TYPE_KEY = "@envelope.type"
        const val ENVELOPE_DATA_TYPE_KEY = "@envelope.dataType"
        const val ENVELOPE_DESCRIPTION_KEY = "@envelope.description"
        const val ENVELOPE_TIME_KEY = "@envelope.time"
    }
}
