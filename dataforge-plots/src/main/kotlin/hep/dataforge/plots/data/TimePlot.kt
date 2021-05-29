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
package hep.dataforge.plots.data

import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.Meta
import hep.dataforge.plots.Plottable
import hep.dataforge.plots.data.TimePlot.Companion.MAX_AGE_KEY
import hep.dataforge.plots.data.TimePlot.Companion.MAX_ITEMS_KEY
import hep.dataforge.plots.data.TimePlot.Companion.PREF_ITEMS_KEY
import hep.dataforge.tables.Adapters
import hep.dataforge.utils.DateTimeUtils
import hep.dataforge.values.Value
import hep.dataforge.values.ValueMap
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * A plottable to display dynamic series with limited number of elements (x axis is always considered to be time). Both
 * criteria are used to eviction of old elements
 *
 * @author Alexander Nozik
 */
@ValueDefs(
        ValueDef(key = MAX_AGE_KEY, type = [ValueType.NUMBER], def = "-1", info = "The maximum age of items in milliseconds. Negative means no limit"),
        ValueDef(key = MAX_ITEMS_KEY, type = [ValueType.NUMBER], def = "1000", info = "The maximum number of items. Negative means no limit"),
        ValueDef(key = PREF_ITEMS_KEY, type = [ValueType.NUMBER], def = "400", info = "The preferred number of items to leave after cleanup.")
)
class TimePlot(name: String, val yKey: String = Adapters.Y_AXIS, meta: Meta = Meta.empty(), private val timestampKey: String = TIMESTAMP_KEY) :
        XYPlot(name, meta, Adapters.buildXYAdapter(timestampKey, yKey)) {

    private val map = TreeMap<Instant, Values>()

    /**
     * Puts value with the same name as this y name from data point. If data
     * point contains time, it is used, otherwise current time is used.
     *
     * @param point
     */
    fun put(point: Values) {
        val v = point.getValue(yKey)
        if (point.hasValue(timestampKey)) {
            put(point.getValue(timestampKey).time, v)
        } else {
            put(v)
        }
    }

    /**
     * Put value with current time
     *
     * @param value
     */
    fun put(value: Value) {
        put(DateTimeUtils.now(), value)
    }

    /**
     * Put time-value pair
     *
     * @param time
     * @param value
     */
    fun put(time: Instant, value: Value) {
        val point = HashMap<String, Value>(2)
        point[timestampKey] = Value.of(time)
        point[yKey] = value
        synchronized(this) {
            this.map[time] = ValueMap(point)
        }

        if (size() > 2) {
            val maxItems = config.getInt(MAX_ITEMS_KEY, -1)
            val prefItems = config.getInt(PREF_ITEMS_KEY, Math.min(400, maxItems))
            val maxAge = config.getInt(MAX_AGE_KEY, -1)
            cleanup(maxAge, maxItems, prefItems)
        }

        notifyDataChanged()
    }

    fun putAll(items: Iterable<Pair<Instant, Value>>) {
        items.forEach{pair->
            map[pair.first] = ValueMap.ofPairs(
                    timestampKey to pair.first,
                    yKey to pair.second
            )
        }

        if (size() > 2) {
            val maxItems = config.getInt(MAX_ITEMS_KEY, -1)
            val prefItems = config.getInt(PREF_ITEMS_KEY, Math.min(400, maxItems))
            val maxAge = config.getInt(MAX_AGE_KEY, -1)
            cleanup(maxAge, maxItems, prefItems)
        }

        notifyDataChanged()
    }

    override fun getRawData(query: Meta): List<Values> {
        return ArrayList(map.values)
    }

    fun setMaxItems(maxItems: Int) {
        config.setValue(MAX_ITEMS_KEY, maxItems)
    }

    fun setMaxAge(age: Duration) {
        config.setValue(MAX_AGE_KEY, age.toMillis())
    }

    fun setPrefItems(prefItems: Int) {
        configureValue(PREF_ITEMS_KEY, prefItems)
    }

    fun size(): Int {
        return map.size
    }

    fun clear() {
        this.map.clear()
        notifyDataChanged()
    }

    private fun cleanup(maxAge: Int, maxItems: Int, prefItems: Int) {
        val first = map.firstKey()
        val last = map.lastKey()

        val oldsize = size()
        if (maxItems in 1..(oldsize - 1)) {
            //copying retained elements into new map
            val newMap = TreeMap<Instant, Values>()
            val step = (Duration.between(first, last).toMillis() / prefItems).toInt()
            newMap[first] = map.firstEntry().value
            newMap[last] = map.lastEntry().value
            var x = first
            while (x.isBefore(last!!)) {
                val entry = map.ceilingEntry(x)
                newMap.putIfAbsent(entry.key, entry.value)
                x = x.plusMillis(step.toLong())
            }
            //replacing map with new one
            synchronized(this) {
                this.map.clear()
                this.map.putAll(newMap)
            }
            LoggerFactory.getLogger(javaClass).debug("Reduced size from {} to {}", oldsize, size())
        }

        synchronized(this) {
            while (maxAge > 0 && last != null && Duration.between(map.firstKey(), last).toMillis() > maxAge) {
                map.remove(map.firstKey())
            }
        }
    }

    companion object {

        fun setMaxItems(plot: Plottable, maxItems: Int) {
            plot.configureValue(MAX_ITEMS_KEY, maxItems)
        }

        fun setMaxAge(plot: Plottable, age: Duration) {
            plot.configureValue(MAX_AGE_KEY, age.toMillis())
        }

        fun setPrefItems(plot: Plottable, prefItems: Int) {
            plot.configureValue(PREF_ITEMS_KEY, prefItems)
        }

        fun put(plot: Plottable, value: Any) {
            if (plot is TimePlot) {
                plot.put(Value.of(value))
            } else {
                LoggerFactory.getLogger(TimePlot::class.java).warn("Trying to put value TimePlot value into different plot")
            }
        }

        const val MAX_AGE_KEY = "maxAge"
        const val MAX_ITEMS_KEY = "maxItems"
        const val PREF_ITEMS_KEY = "prefItems"

        const val TIMESTAMP_KEY = "timestamp"
    }


}
