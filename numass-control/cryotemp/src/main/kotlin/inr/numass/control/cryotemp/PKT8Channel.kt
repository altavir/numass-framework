/*
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package inr.numass.control.cryotemp

import hep.dataforge.Named
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.stringValue
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.Metoid


internal fun createChannel(name: String): PKT8Channel =
        PKT8Channel(MetaBuilder("channel").putValue("name", name)) { d -> d }

internal fun createChannel(meta: Meta): PKT8Channel {
    val transformationType = meta.getString("transformationType", "default")
    if (meta.hasValue("coefs")) {
        when (transformationType) {
            "default", "hyperbolic" -> {
                val coefs = meta.getValue("coefs").list
                val r0 = meta.getDouble("r0", 1000.0)
                return PKT8Channel(meta) { r ->
                    coefs.indices.sumByDouble { coefs[it].double * Math.pow(r0 / r, it.toDouble()) }
                }
            }
            else -> throw RuntimeException("Unknown transformation type")
        }
    } else {
        //identity transformation
        return PKT8Channel(meta) { d -> d }
    }
}

/**
 * Created by darksnake on 28-Sep-16.
 */
class PKT8Channel(override val meta: Meta, private val func: (Double) -> Double) : Named, Metoid {

    override val name: String by meta.stringValue()

    fun description(): String {
        return meta.getString("description", "")
    }

    /**
     * @param r negative if temperature transformation not defined
     * @return
     */
    fun getTemperature(r: Double): Double {
        return func(r)
    }

    fun evaluate(r: Double): Meta {
        return buildMeta {
            "channel" to name
            "raw" to r
            "temperature" to getTemperature(r)
        }
    }

}