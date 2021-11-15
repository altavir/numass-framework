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
import hep.dataforge.values.*
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_ERROR_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.LENGTH_KEY
import inr.numass.data.analyzers.TimeAnalyzer.AveragingMethod.*
import inr.numass.data.api.*
import inr.numass.data.api.NumassPoint.Companion.HV_KEY
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.set
import kotlin.math.*
import kotlin.streams.asSequence


/**
 * An analyzer which uses time information from events
 * Created by darksnake on 11.07.2017.
 */
@ValueDefs(
    ValueDef(
        key = "separateParallelBlocks",
        type = [ValueType.BOOLEAN],
        info = "If true, then parallel blocks will be forced to be evaluated separately"
    ),
    ValueDef(
        key = "chunkSize",
        type = [ValueType.NUMBER],
        def = "-1",
        info = "The number of events in chunk to split the chain into. If negative, no chunks are used"
    )
)
open class TimeAnalyzer(processor: SignalProcessor? = null) : AbstractAnalyzer(processor) {

    override fun analyze(block: NumassBlock, config: Meta): Values {
        //In case points inside points
        if (block is ParentBlock && (block.isSequential || config.getBoolean("separateParallelBlocks", false))) {
            return analyzeParent(block, config)
        }

        val t0 = getT0(block, config).toLong()

        val chunkSize = config.getInt("chunkSize", -1)

        val count = super.getEvents(block, config).count()
        val length = block.length.toNanos().toDouble() / 1e9

        val res = when {
            count < 1000 -> ValueMap.ofPairs(
                LENGTH_KEY to length,
                COUNT_KEY to count,
                COUNT_RATE_KEY to count.toDouble() / length,
                COUNT_RATE_ERROR_KEY to sqrt(count.toDouble()) / length
            )
            chunkSize > 0 -> getEventsWithDelay(block, config)
                .chunked(chunkSize) { analyzeSequence(it.asSequence(), t0) }
                .toList()
                .mean(config.getEnum("mean", WEIGHTED))
            else -> analyzeSequence(getEventsWithDelay(block, config), t0)
        }

        return ValueMap.Builder(res)
            .putValue("blockLength", length)
            .putValue(NumassAnalyzer.WINDOW_KEY, config.getRange())
            .putValue(NumassAnalyzer.TIME_KEY, block.startTime)
            .putValue(T0_KEY, t0.toDouble() / 1000.0)
            .build()
    }


    private fun analyzeSequence(sequence: Sequence<Pair<NumassEvent, Long>>, t0: Long): Values {
        val totalN = AtomicLong(0)
        val totalT = AtomicLong(0)
        sequence.filter { pair -> pair.second >= t0 }
            .forEach { pair ->
                totalN.incrementAndGet()
                //TODO add progress listener here
                totalT.addAndGet(pair.second.toLong())
            }

        if (totalN.toInt() == 0) {
            error("Zero number of intervals")
        }

        val countRate = 1e6 * totalN.get() / (totalT.get() / 1000 - t0.toLong() * totalN.get() / 1000)
        //1e9 / (totalT.get() / totalN.get() - t0);
        val countRateError = countRate / sqrt(totalN.get().toDouble())
        val length = totalT.get() / 1e9
        val count = (length * countRate).toLong()

        return ValueMap.ofPairs(
            LENGTH_KEY to length,
            COUNT_KEY to count,
            COUNT_RATE_KEY to countRate,
            COUNT_RATE_ERROR_KEY to countRateError
        )

    }

    override fun analyzeParent(point: ParentBlock, config: Meta): Values {
        //Average count rates, do not sum events
        val res = point.blocks.map { it -> analyze(it, config) }

        val map = HashMap(res.mean(config.getEnum("mean", WEIGHTED)).asMap())
        if (point is NumassPoint) {
            map[HV_KEY] = Value.of(point.voltage)
        }
        return ValueMap(map)
    }

    enum class AveragingMethod {
        ARITHMETIC,
        WEIGHTED,
        GEOMETRIC
    }

    /**
     * Combine multiple blocks from the same point into one
     *
     * @return
     */
    private fun List<Values>.mean(method: AveragingMethod): Values {

        if (this.isEmpty()) {
            return ValueMap.Builder()
                .putValue(LENGTH_KEY, 0)
                .putValue(COUNT_KEY, 0)
                .putValue(COUNT_RATE_KEY, 0)
                .putValue(COUNT_RATE_ERROR_KEY, 0)
                .build()
        }

        val totalTime = sumOf { it.getDouble(LENGTH_KEY) }

        val (countRate, countRateDispersion) = when (method) {
            ARITHMETIC -> Pair(
                sumOf { it.getDouble(COUNT_RATE_KEY) } / size,
                sumOf { it.getDouble(COUNT_RATE_ERROR_KEY).pow(2.0) } / size / size
            )
            WEIGHTED -> Pair(
                sumOf { it.getDouble(COUNT_RATE_KEY) * it.getDouble(LENGTH_KEY) } / totalTime,
                sumOf { (it.getDouble(COUNT_RATE_ERROR_KEY) * it.getDouble(LENGTH_KEY) / totalTime).pow(2.0) }
            )
            GEOMETRIC -> {
                val mean = exp(sumOf { ln(it.getDouble(COUNT_RATE_KEY)) } / size)
                val variance = (mean / size).pow(2.0) * sumOf {
                    (it.getDouble(COUNT_RATE_ERROR_KEY) / it.getDouble(
                        COUNT_RATE_KEY
                    )).pow(2.0)
                }
                Pair(mean, variance)
            }
        }

        return ValueMap.Builder(first())
            .putValue(LENGTH_KEY, totalTime)
            .putValue(COUNT_KEY, sumOf { it.getInt(COUNT_KEY) })
            .putValue(COUNT_RATE_KEY, countRate)
            .putValue(COUNT_RATE_ERROR_KEY, sqrt(countRateDispersion))
            .build()
    }

    @ValueDefs(
        ValueDef(key = "t0", type = arrayOf(ValueType.NUMBER), info = "Constant t0 cut"),
        ValueDef(
            key = "t0.crFraction",
            type = arrayOf(ValueType.NUMBER),
            info = "The relative fraction of events that should be removed by time cut"
        ),
        ValueDef(key = "t0.min", type = arrayOf(ValueType.NUMBER), def = "0", info = "Minimal t0")
    )
    protected fun getT0(block: NumassBlock, meta: Meta): Int {
        return if (meta.hasValue("t0")) {
            meta.getInt("t0")
        } else if (meta.hasMeta("t0")) {
            val fraction = meta.getDouble("t0.crFraction")
            val cr = estimateCountRate(block)
            if (cr < meta.getDouble("t0.minCR", 0.0)) {
                0
            } else {
                max(-1e9 / cr * ln(1.0 - fraction), meta.getDouble("t0.min", 0.0)).toInt()
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
    fun getEventsWithDelay(block: NumassBlock, config: Meta): Sequence<Pair<NumassEvent, Long>> {
        val inverted = config.getBoolean("inverted", true)
        //range is included in super.getEvents
        val events = super.getEvents(block, config).toMutableList()

        if (config.getBoolean("sortEvents", false) || (block is ParentBlock && !block.isSequential)) {
            //sort in place if needed
            events.sortBy { it.timeOffset }
        }

        return events.asSequence().zipWithNext { prev, next ->
            val delay = max(next.timeOffset - prev.timeOffset, 0)
            if (inverted) {
                Pair(next, delay)
            } else {
                Pair(prev, delay)
            }
        }
    }

    /**
     * The filtered stream of events
     *
     * @param block
     * @param meta
     * @return
     */
    override fun getEvents(block: NumassBlock, meta: Meta): List<NumassEvent> {
        val t0 = getT0(block, meta)
        return getEventsWithDelay(block, meta)
            .filter { pair -> pair.second >= t0 }
            .map { it.first }.toList()
    }

    public override fun getTableFormat(config: Meta): TableFormat {
        return TableFormatBuilder()
            .addNumber(HV_KEY, X_VALUE_KEY)
            .addNumber(LENGTH_KEY)
            .addNumber(COUNT_KEY)
            .addNumber(COUNT_RATE_KEY, Y_VALUE_KEY)
            .addNumber(COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
            .addColumn(NumassAnalyzer.WINDOW_KEY)
            .addTime()
            .addNumber(T0_KEY)
            .build()
    }

    companion object {
        const val T0_KEY = "t0"

        val NAME_LIST = arrayOf(
            LENGTH_KEY,
            COUNT_KEY,
            COUNT_RATE_KEY,
            COUNT_RATE_ERROR_KEY,
            NumassAnalyzer.WINDOW_KEY,
            NumassAnalyzer.TIME_KEY,
            T0_KEY
        )
    }
}
