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
package inr.numass.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.tables.Adapters.*
import hep.dataforge.tables.BasicAdapter
import hep.dataforge.tables.ValueMap
import hep.dataforge.tables.ValuesAdapter
import hep.dataforge.values.Value
import hep.dataforge.values.Values
import java.util.*
import java.util.stream.Stream

/**
 * @author Darksnake
 */
class SpectrumAdapter : BasicAdapter {

    constructor(meta: Meta) : super(meta) {}

    constructor(xName: String, yName: String, yErrName: String, measurementTime: String) : super(MetaBuilder(ValuesAdapter.ADAPTER_KEY)
            .setValue(X_VALUE_KEY, xName)
            .setValue(Y_VALUE_KEY, yName)
            .setValue(Y_ERROR_KEY, yErrName)
            .setValue(POINT_LENGTH_NAME, measurementTime)
            .build()
    ) {
    }

    constructor(xName: String, yName: String, measurementTime: String) : super(MetaBuilder(ValuesAdapter.ADAPTER_KEY)
            .setValue(X_VALUE_KEY, xName)
            .setValue(Y_VALUE_KEY, yName)
            .setValue(POINT_LENGTH_NAME, measurementTime)
            .build()
    ) {
    }

    fun getTime(point: Values): Double {
        return this.optComponent(point, POINT_LENGTH_NAME).map<Double> { it.getDouble() }.orElse(1.0)
    }

    fun buildSpectrumDataPoint(x: Double, count: Long, t: Double): Values {
        return ValueMap.of(arrayOf(getComponentName(X_VALUE_KEY), getComponentName(Y_VALUE_KEY), getComponentName(POINT_LENGTH_NAME)),
                x, count, t)
    }

    fun buildSpectrumDataPoint(x: Double, count: Long, countErr: Double, t: Double): Values {
        return ValueMap.of(arrayOf(getComponentName(X_VALUE_KEY), getComponentName(Y_VALUE_KEY), getComponentName(Y_ERROR_KEY), getComponentName(POINT_LENGTH_NAME)),
                x, count, countErr, t)
    }


    override fun optComponent(values: Values, component: String): Optional<Value> {
        when (component) {
            "count" -> return super.optComponent(values, Y_VALUE_KEY)
            Y_VALUE_KEY -> return super.optComponent(values, Y_VALUE_KEY)
                    .map { it -> it.getDouble() / getTime(values) }
                    .map { Value.of(it) }
            Y_ERROR_KEY -> {
                val err = super.optComponent(values, Y_ERROR_KEY)
                return if (err.isPresent) {
                    Optional.of(Value.of(err.get().getDouble() / getTime(values)))
                } else {
                    val y = getComponent(values, Y_VALUE_KEY).getDouble()
                    if (y < 0) {
                        Optional.empty()
                    } else if (y == 0.0) {
                        //avoid infinite weights
                        Optional.of(Value.of(1.0 / getTime(values)))
                    } else {
                        Optional.of(Value.of(Math.sqrt(y) / getTime(values)))
                    }
                }
            }

            else -> return super.optComponent(values, component)
        }
    }

    override fun listComponents(): Stream<String> {
        return Stream.concat(super.listComponents(), Stream.of(X_VALUE_KEY, Y_VALUE_KEY, POINT_LENGTH_NAME)).distinct()
    }

    companion object {
        private const val POINT_LENGTH_NAME = "time"
    }
}
