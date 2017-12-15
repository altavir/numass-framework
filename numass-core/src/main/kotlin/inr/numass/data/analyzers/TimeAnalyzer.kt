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

package inr.numass.data.analyzers

import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.Meta
import hep.dataforge.tables.Adapters.*
import hep.dataforge.tables.TableFormat
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.tables.ValueMap
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassPoint.HV_KEY
import inr.numass.data.api.SignalProcessor
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.streams.asStream

/**
 * An analyzer which uses time information from events
 * Created by darksnake on 11.07.2017.
 */
class TimeAnalyzer @JvmOverloads constructor(private val processor: SignalProcessor? = null) : AbstractAnalyzer(processor) {

    override fun analyze(block: NumassBlock, config: Meta): Values {
        //In case points inside points
        if (block is NumassPoint) {
            return analyzePoint(block, config)
        }


        val loChannel = config.getInt("window.lo", 0)
        val upChannel = config.getInt("window.up", Integer.MAX_VALUE)
        val t0 = getT0(block, config).toLong()

        val totalN = AtomicLong(0)
        val totalT = AtomicLong(0)

        getEventsWithDelay(block, config)
                .filter { pair -> pair.second >= t0 }
                .forEach { pair ->
                    totalN.incrementAndGet()
                    //TODO add progress listener here
                    totalT.addAndGet(pair.second)
                }

        val countRate = 1e6 * totalN.get() / (totalT.get() / 1000 - t0 * totalN.get() / 1000)//1e9 / (totalT.get() / totalN.get() - t0);
        val countRateError = countRate / Math.sqrt(totalN.get().toDouble())
        val length = totalT.get() / 1e9
        val count = (length * countRate).toLong()

        return ValueMap.of(NAME_LIST,
                length,
                count,
                countRate,
                countRateError,
                arrayOf(loChannel, upChannel),
                block.startTime,
                t0.toDouble() / 1000.0
        )
    }

    override fun analyzePoint(point: NumassPoint, config: Meta): Values {
        //Average count rates, do not sum events
        val res = point.blocks
                .map { it -> analyze(it, config) }
                .reduce(null) { v1, v2 -> this.combineBlockResults(v1, v2) }

        val map = HashMap(res.asMap())
        map.put(HV_KEY, Value.of(point.voltage))
        return ValueMap(map)
    }

    /**
     * Combine two blocks from the same point into one
     *
     * @param v1
     * @param v2
     * @return
     */
    private fun combineBlockResults(v1: Values?, v2: Values?): Values? {
        if (v1 == null) {
            return v2
        }
        if (v2 == null) {
            return v1
        }


        val t1 = v1.getDouble(NumassAnalyzer.LENGTH_KEY)
        val t2 = v2.getDouble(NumassAnalyzer.LENGTH_KEY)
        val cr1 = v1.getDouble(NumassAnalyzer.COUNT_RATE_KEY)
        val cr2 = v2.getDouble(NumassAnalyzer.COUNT_RATE_KEY)
        val err1 = v1.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY)
        val err2 = v2.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY)

        val countRate = (t1 * cr1 + t2 * cr2) / (t1 + t2)

        val countRateErr = Math.sqrt(Math.pow(t1 * err1 / (t1 + t2), 2.0) + Math.pow(t2 * err2 / (t1 + t2), 2.0))


        return ValueMap.of(NAME_LIST,
                v1.getDouble(NumassAnalyzer.LENGTH_KEY) + v2.getDouble(NumassAnalyzer.LENGTH_KEY),
                v1.getInt(NumassAnalyzer.COUNT_KEY) + v2.getInt(NumassAnalyzer.COUNT_KEY),
                countRate,
                countRateErr,
                v1.getValue(NumassAnalyzer.WINDOW_KEY),
                v1.getValue(NumassAnalyzer.TIME_KEY),
                v1.getDouble(T0_KEY)
        )
    }

    @ValueDefs(
            ValueDef(name = "t0", type = arrayOf(ValueType.NUMBER), info = "Constant t0 cut"),
            ValueDef(name = "t0.crFraction", type = arrayOf(ValueType.NUMBER), info = "The relative fraction of events that should be removed by time cut"),
            ValueDef(name = "t0.min", type = arrayOf(ValueType.NUMBER), def = "0", info = "Minimal t0")
    )
    private fun getT0(block: NumassBlock, meta: Meta): Int {
        return if (meta.hasValue("t0")) {
            meta.getInt("t0")!!
        } else if (meta.hasMeta("t0")) {
            val fraction = meta.getDouble("t0.crFraction")!!
            val cr = estimateCountRate(block)
            if (cr < meta.getDouble("t0.minCR", 0.0)) {
                0
            } else {
                Math.max(-1e9 / cr * Math.log(1.0 - fraction), meta.getDouble("t0.min", 0.0)!!).toInt()
            }
        } else {
            0
        }

    }

    private fun estimateCountRate(block: NumassBlock): Double {
        return block.events.count().toDouble() / block.length.toMillis() * 1000
    }

    fun zipEvents(block: NumassBlock, config: Meta): Sequence<Pair<NumassEvent, NumassEvent>> {
        return getAllEvents(block).asSequence().zipWithNext()
    }

    /**
     * The chain of event with delays in nanos
     *
     * @param block
     * @param config
     * @return
     */
    fun getEventsWithDelay(block: NumassBlock, config: Meta): Stream<Pair<NumassEvent, Long>> {
        return super.getEvents(block, config).asSequence().zipWithNext { prev, next ->
            val delay = Math.max(next.timeOffset - prev.timeOffset, 0)
            Pair(prev, delay)
        }.asStream()
    }

    /**
     * The filtered stream of events
     *
     * @param block
     * @param meta
     * @return
     */
    override fun getEvents(block: NumassBlock, meta: Meta): Stream<NumassEvent> {
        val t0 = getT0(block, meta).toLong()
        return getEventsWithDelay(block, meta).filter { pair -> pair.second >= t0 }.map { it.first }
    }

    public override fun getTableFormat(config: Meta): TableFormat {
        return TableFormatBuilder()
                .addNumber(HV_KEY, X_VALUE_KEY)
                .addNumber(NumassAnalyzer.LENGTH_KEY)
                .addNumber(NumassAnalyzer.COUNT_KEY)
                .addNumber(NumassAnalyzer.COUNT_RATE_KEY, Y_VALUE_KEY)
                .addNumber(NumassAnalyzer.COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
                .addColumn(NumassAnalyzer.WINDOW_KEY)
                .addTime()
                .addNumber(T0_KEY)
                .build()
    }

    companion object {
        const val T0_KEY = "t0"

        val NAME_LIST = arrayOf(
                NumassAnalyzer.LENGTH_KEY,
                NumassAnalyzer.COUNT_KEY,
                NumassAnalyzer.COUNT_RATE_KEY,
                NumassAnalyzer.COUNT_RATE_ERROR_KEY,
                NumassAnalyzer.WINDOW_KEY,
                NumassAnalyzer.TIME_KEY,
                T0_KEY
        )
    }
}
