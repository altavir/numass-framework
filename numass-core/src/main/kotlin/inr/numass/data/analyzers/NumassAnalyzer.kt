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

import hep.dataforge.meta.Meta
import hep.dataforge.tables.*
import hep.dataforge.tables.Adapters.*
import hep.dataforge.values.Value
import hep.dataforge.values.ValueMap
import hep.dataforge.values.Values
import inr.numass.data.api.*
import inr.numass.data.api.NumassPoint.Companion.HV_KEY
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.streams.asSequence

/**
 * A general raw data analysis utility. Could have different implementations
 * Created by darksnake on 06-Jul-17.
 */
interface NumassAnalyzer {

    /**
     * Perform analysis on block. The values for count rate, its error and point length in nanos must
     * exist, but occasionally additional values could also be presented.
     *
     * @param block
     * @return
     */
    fun analyze(block: NumassBlock, config: Meta = Meta.empty()): Values

    /**
     * Analysis result for point including hv information
     * @param point
     * @param config
     * @return
     */
    fun analyzeParent(point: ParentBlock, config: Meta = Meta.empty()): Values {
        val map = HashMap(analyze(point, config).asMap())
        if(point is NumassPoint) {
            map[HV_KEY] = Value.of(point.voltage)
        }
        return ValueMap(map)
    }

    /**
     * Return unsorted stream of events including events from frames
     *
     * @param block
     * @return
     */
    fun getEvents(block: NumassBlock, meta: Meta = Meta.empty()): Stream<NumassEvent>

    /**
     * Analyze the whole set. And return results as a table
     *
     * @param set
     * @param config
     * @return
     */
    fun analyzeSet(set: NumassSet, config: Meta): Table

    /**
     * Get the approximate number of events in block. Not all analyzers support precise event counting
     *
     * @param block
     * @param config
     * @return
     */
    fun getCount(block: NumassBlock, config: Meta): Long {
        return analyze(block, config).getValue(COUNT_KEY).number.toLong()
    }

    /**
     * Get approximate effective point length in nanos. It is not necessary corresponds to real point length.
     *
     * @param block
     * @param config
     * @return
     */
    fun getLength(block: NumassBlock, config: Meta = Meta.empty()): Long {
        return analyze(block, config).getValue(LENGTH_KEY).number.toLong()
    }

    fun getAmplitudeSpectrum(block: NumassBlock, config: Meta = Meta.empty()): Table {
        val seconds = block.length.toMillis().toDouble() / 1000.0
        return getAmplitudeSpectrum(getEvents(block, config).asSequence(), seconds, config)
    }

    companion object {
        const val CHANNEL_KEY = "channel"
        const val COUNT_KEY = "count"
        const val LENGTH_KEY = "length"
        const val COUNT_RATE_KEY = "cr"
        const val COUNT_RATE_ERROR_KEY = "crErr"

        const val WINDOW_KEY = "window"
        const val TIME_KEY = "timestamp"

        val DEFAULT_ANALYZER: NumassAnalyzer = SmartAnalyzer()

        val AMPLITUDE_ADAPTER: ValuesAdapter = Adapters.buildXYAdapter(CHANNEL_KEY, COUNT_RATE_KEY)

//        val MAX_CHANNEL = 10000
    }
}

/**
 * Calculate number of counts in the given channel
 *
 * @param spectrum
 * @param loChannel
 * @param upChannel
 * @return
 */
fun Table.countInWindow(loChannel: Short, upChannel: Short): Long {
    return this.rows.filter { row ->
        row.getInt(NumassAnalyzer.CHANNEL_KEY) in loChannel..(upChannel - 1)
    }.mapToLong { it -> it.getValue(NumassAnalyzer.COUNT_KEY).number.toLong() }.sum()
}

/**
 * Calculate the amplitude spectrum for a given block. The s
 *
 * @param events
 * @param length length in seconds, used for count rate calculation
 * @param config
 * @return
 */
fun getAmplitudeSpectrum(events: Sequence<NumassEvent>, length: Double, config: Meta = Meta.empty()): Table {
    val format = TableFormatBuilder()
            .addNumber(NumassAnalyzer.CHANNEL_KEY, X_VALUE_KEY)
            .addNumber(NumassAnalyzer.COUNT_KEY)
            .addNumber(NumassAnalyzer.COUNT_RATE_KEY, Y_VALUE_KEY)
            .addNumber(NumassAnalyzer.COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
            .updateMeta { metaBuilder -> metaBuilder.setNode("config", config) }
            .build()

    //optimized for fastest computation
    val spectrum: MutableMap<Int, AtomicLong> = HashMap()
    events.forEach { event ->
        val channel = event.amp.toInt()
        spectrum.getOrPut(channel) {
            AtomicLong(0)
        }.incrementAndGet()
    }


    val minChannel = config.getInt("window.lo") { spectrum.keys.min()?:0 }
    val maxChannel = config.getInt("window.up") { spectrum.keys.max()?: 4096 }

    return ListTable.Builder(format)
            .rows(IntStream.range(minChannel, maxChannel)
                    .mapToObj { i ->
                        val value = spectrum[i]?.get() ?: 0
                        ValueMap.of(
                                format.namesAsArray(),
                                i,
                                value,
                                value.toDouble() / length,
                                Math.sqrt(value.toDouble()) / length
                        )
                    }
            ).build()
}

/**
 * Apply window and binning to a spectrum. Empty bins are filled with zeroes
 *
 * @param binSize
 * @param loChannel autodefined if negative
 * @param upChannel autodefined if negative
 * @return
 */
@JvmOverloads
fun Table.withBinning(binSize: Int, loChannel: Int? = null, upChannel: Int? = null): Table {
    val format = TableFormatBuilder()
            .addNumber(NumassAnalyzer.CHANNEL_KEY, X_VALUE_KEY)
            .addNumber(NumassAnalyzer.COUNT_KEY, Y_VALUE_KEY)
            .addNumber(NumassAnalyzer.COUNT_RATE_KEY)
            .addNumber(NumassAnalyzer.COUNT_RATE_ERROR_KEY)
            .addNumber("binSize")
    val builder = ListTable.Builder(format)

    var chan = loChannel
            ?: this.getColumn(NumassAnalyzer.CHANNEL_KEY).stream().mapToInt { it.int }.min().orElse(0)

    val top = upChannel
            ?: this.getColumn(NumassAnalyzer.CHANNEL_KEY).stream().mapToInt { it.int }.max().orElse(1)

    while (chan < top - binSize) {
        val count = AtomicLong(0)
        val countRate = AtomicReference(0.0)
        val countRateDispersion = AtomicReference(0.0)

        val binLo = chan
        val binUp = chan + binSize

        this.rows.filter { row ->
            row.getInt(NumassAnalyzer.CHANNEL_KEY) in binLo..(binUp - 1)
        }.forEach { row ->
            count.addAndGet(row.getValue(NumassAnalyzer.COUNT_KEY, 0).long)
            countRate.accumulateAndGet(row.getDouble(NumassAnalyzer.COUNT_RATE_KEY, 0.0)) { d1, d2 -> d1 + d2 }
            countRateDispersion.accumulateAndGet(Math.pow(row.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY, 0.0), 2.0)) { d1, d2 -> d1 + d2 }
        }
        val bin = Math.min(binSize, top - chan)
        builder.row(chan.toDouble() + bin.toDouble() / 2.0, count.get(), countRate.get(), Math.sqrt(countRateDispersion.get()), bin)
        chan += binSize
    }
    return builder.build()
}

/**
 * Subtract reference spectrum.
 *
 * @param sp1
 * @param sp2
 * @return
 */
fun subtractAmplitudeSpectrum(sp1: Table, sp2: Table): Table {
    val format = TableFormatBuilder()
            .addNumber(NumassAnalyzer.CHANNEL_KEY, X_VALUE_KEY)
            .addNumber(NumassAnalyzer.COUNT_RATE_KEY, Y_VALUE_KEY)
            .addNumber(NumassAnalyzer.COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
            .build()

    val builder = ListTable.Builder(format)

    sp1.forEach { row1 ->
        val channel = row1.getDouble(NumassAnalyzer.CHANNEL_KEY)
        val row2 = sp2.rows.asSequence().find { it.getDouble(NumassAnalyzer.CHANNEL_KEY) == channel }   //t2[channel]
        if (row2 == null) {
            throw RuntimeException("Reference for channel $channel not found");

        } else {
            val value = Math.max(row1.getDouble(NumassAnalyzer.COUNT_RATE_KEY) - row2.getDouble(NumassAnalyzer.COUNT_RATE_KEY), 0.0)
            val error1 = row1.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY)
            val error2 = row2.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY)
            val error = Math.sqrt(error1 * error1 + error2 * error2)
            builder.row(channel, value, error)
        }
    }
    return builder.build()
}