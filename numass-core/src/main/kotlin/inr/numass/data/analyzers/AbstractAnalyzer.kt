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
import hep.dataforge.tables.Adapters.*
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableFormat
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.toList
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.NumassPoint.Companion.HV_KEY
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SignalProcessor
import java.util.stream.Stream

/**
 * Created by darksnake on 11.07.2017.
 */
abstract class AbstractAnalyzer @JvmOverloads constructor(private val processor: SignalProcessor? = null) :
    NumassAnalyzer {

    /**
     * Return unsorted stream of events including events from frames.
     * In theory, events after processing could be unsorted due to mixture of frames and events.
     * In practice usually block have either frame or events, but not both.
     *
     * @param block
     * @return
     */
    override fun getEvents(block: NumassBlock, meta: Meta): List<NumassEvent> {
        val range = meta.getRange()
        return getAllEvents(block).filter { event ->
            event.amplitude.toInt() in range
        }.toList()
    }

    protected fun Meta.getRange(): IntRange {
        val loChannel = getInt("window.lo", 0)
        val upChannel = getInt("window.up", Integer.MAX_VALUE)
        return loChannel until upChannel
    }

    protected fun getAllEvents(block: NumassBlock): Stream<NumassEvent> {
        return when {
            block.frames.count() == 0L -> block.events
            processor == null -> throw IllegalArgumentException("Signal processor needed to analyze frames")
            else -> Stream.concat(block.events, block.frames.flatMap { processor.process(block, it) })
        }
    }

    /**
     * Get table format for summary table
     *
     * @param config
     * @return
     */
    protected open fun getTableFormat(config: Meta): TableFormat {
        return TableFormatBuilder()
            .addNumber(HV_KEY, X_VALUE_KEY)
            .addNumber(NumassAnalyzer.LENGTH_KEY)
            .addNumber(NumassAnalyzer.COUNT_KEY)
            .addNumber(NumassAnalyzer.COUNT_RATE_KEY, Y_VALUE_KEY)
            .addNumber(NumassAnalyzer.COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
            .addColumn(NumassAnalyzer.WINDOW_KEY)
            .addTime()
            .build()
    }


    override fun analyzeSet(set: NumassSet, config: Meta): Table {
        val format = getTableFormat(config)

        return ListTable.Builder(format)
            .rows(set.points.map { point -> analyzeParent(point, config) })
            .build()
    }

    companion object {
        val NAME_LIST = arrayOf(
            NumassAnalyzer.LENGTH_KEY,
            NumassAnalyzer.COUNT_KEY,
            NumassAnalyzer.COUNT_RATE_KEY,
            NumassAnalyzer.COUNT_RATE_ERROR_KEY,
            NumassAnalyzer.WINDOW_KEY,
            NumassAnalyzer.TIME_KEY
        )
    }
}
