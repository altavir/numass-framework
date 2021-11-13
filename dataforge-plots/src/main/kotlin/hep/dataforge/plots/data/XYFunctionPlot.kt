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
package hep.dataforge.plots.data

import hep.dataforge.asName
import hep.dataforge.description.Descriptors
import hep.dataforge.description.NodeDescriptor
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.*
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Adapters.DEFAULT_XY_ADAPTER
import hep.dataforge.tables.Adapters.buildXYDataPoint
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType.BOOLEAN
import hep.dataforge.values.ValueType.NUMBER
import hep.dataforge.values.Values
import java.util.*
import kotlin.collections.set

/**
 * A class for dynamic function values calculation for plot
 *
 * @author Alexander Nozik
 */
@ValueDefs(
    ValueDef(key = "showLine", type = arrayOf(BOOLEAN), def = "true", info = "Show the connecting line."),
    ValueDef(key = "showSymbol", type = arrayOf(BOOLEAN), def = "false", info = "Show symbols for data point."),
    ValueDef(key = "showErrors", type = arrayOf(BOOLEAN), def = "false", info = "Show errors for points."),
    ValueDef(key = "range.from", type = arrayOf(NUMBER), def = "0.0", info = "Lower boundary for calculation range"),
    ValueDef(key = "range.to", type = arrayOf(NUMBER), def = "1.0", info = "Upper boundary for calculation range"),
    ValueDef(key = "density", type = arrayOf(NUMBER), def = "200", info = "Minimal number of points per plot")
)
class XYFunctionPlot(name: String, meta: Meta = Meta.empty(), val function: (Double) -> Double) :
    XYPlot(name, meta, Adapters.DEFAULT_XY_ADAPTER) {

    private val cache = TreeMap<Double, Double>()

    /**
     * The minimal number of points per range
     */

    var density by config.mutableIntValue(def = 200)
    var from by config.mutableDoubleValue("range.from")
    var to by config.mutableDoubleValue("range.to")

    /**
     * Turns line smoothing on or off
     *
     * @param smoothing
     */
    var smoothing by config.customMutableValue("connectionType", read = { it.string == "spline" }) {
        if (it) {
            "spline"
        } else {
            "default"
        }
    }

    var range by config.mutableCustomNode(
        "range",
        read = { Pair(it.getDouble("from"), it.getDouble("to")) },
        write = {
            invalidateCache()
            buildMeta("range", "from" to it.first, "to" to it.second)
        }
    )

    override fun applyValueChange(name: String, oldValue: Value?, newValue: Value?) {
        super.applyValueChange(name, oldValue, newValue)
        if (name == "density") {
            invalidateCache()
        }
    }

    /**
     * Split region into uniform blocks, then check if each block contains at
     * least one cached point and calculate additional point in the center of
     * the block if it does not.
     *
     *
     * If function is not set or desired density not positive does nothing.
     */
    private fun validateCache() {
        // recalculate immutable if boundaries are finite, otherwise use existing immutable
        val nodes = this.density
        if (java.lang.Double.isFinite(from) && java.lang.Double.isFinite(to)) {
            for (i in 0 until nodes) {
                val blockBegin = from + i * (to - from) / (nodes - 1)
                val blockEnd = from + (i + 1) * (to - from) / (nodes - 1)
                if (cache.subMap(blockBegin, blockEnd).isEmpty()) {
                    eval((blockBegin + blockEnd) / 2)
                }
            }
        }
    }

    @Synchronized
    private fun invalidateCache() {
        this.cache.clear()
    }

    /**
     * Calculate function immutable for the given point and return calculated value
     *
     * @param x
     */
    @Synchronized
    private fun eval(x: Double): Double {
        val y = function(x)
        this.cache[x] = y
        return y
    }

    /**
     * Give the fixed point in which this function must be calculated. Calculate value and update range if it does not include point
     *
     * @param x
     */
    fun calculateIn(x: Double): Double {
        if (this.from > x) {
            this.from = x
        }
        if (this.to < x) {
            this.to = x
        }
        return eval(x)
    }

    override val descriptor: NodeDescriptor by lazy {
        Descriptors.forType("plot", this::class)
            .builder()
            .apply { setDefault("connectionType".asName(), ConnectionType.SPLINE) }
            .build()
    }

    override fun getRawData(query: Meta): List<Values> {
        //recalculate immutable with default values
        if (query.hasValue("xRange.from")) {
            this.from = query.getDouble("xRange.from")
        }
        if (query.hasValue("xRange.to")) {
            this.to = query.getDouble("xRange.to")
        }
        if (query.hasValue("density")) {
            this.density = query.getInt("density")
        }
        validateCache()
        return cache.entries.stream()
            .map { entry -> buildXYDataPoint(DEFAULT_XY_ADAPTER, entry.key, entry.value) }
            .toList()
    }

    companion object {

        const val DEFAULT_DENSITY = 200

        @JvmOverloads
        fun plot(
            name: String,
            from: Double,
            to: Double,
            numPoints: Int = DEFAULT_DENSITY,
            meta: Meta = Meta.empty(),
            function: (Double) -> Double,
        ): XYFunctionPlot {
            val p = XYFunctionPlot(name, meta, function)
            p.range = Pair(from, to)
            p.density = numPoints
            return p
        }
    }


}
