/*
 * Copyright  2017 Alexander Nozik.
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

package hep.dataforge.control.devices

import hep.dataforge.context.Context
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import java.time.Duration
import java.time.Instant
import java.util.*

private val VIRTUAL_SENSOR_TYPE = "@test"

private val generator = Random()

class VirtualSensor(context: Context) : Sensor(context, Meta.empty()) {
    override fun startMeasurement(oldMeta: Meta?, newMeta: Meta) {
        if (oldMeta !== newMeta) {
            val delay = Duration.parse(newMeta.getString("duration", "PT0.2S"))
            val mean = newMeta.getDouble("mean", 1.0)
            val sigma = newMeta.getDouble("sigma", 0.1)

            measurement {
                Thread.sleep(delay.toMillis())
                val value = generator.nextDouble() * sigma + mean
                MetaBuilder("result").setValue("value", value).setValue("timestamp", Instant.now())
            }
        }
    }


    override val type: String
        get() {
            return VIRTUAL_SENSOR_TYPE
        }

}