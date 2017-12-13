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
import hep.dataforge.tables.TableFormat
import hep.dataforge.tables.ValueMap
import hep.dataforge.values.Value
import hep.dataforge.values.Values
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.SignalProcessor
import java.util.stream.Stream

/**
 * An analyzer dispatcher which uses different analyzer for different meta
 * Created by darksnake on 11.07.2017.
 */
class SmartAnalyzer(processor: SignalProcessor? = null) : AbstractAnalyzer(processor) {
    private val simpleAnalyzer = SimpleAnalyzer(processor)
    private val debunchAnalyzer = DebunchAnalyzer(processor)
    private val timeAnalyzer = TimeAnalyzer(processor)

    private fun getAnalyzer(config: Meta): NumassAnalyzer {
        return if (config.hasValue("type")) {
            when (config.getString("type")) {
                "simple" -> simpleAnalyzer
                "time" -> timeAnalyzer
                "debunch" -> debunchAnalyzer
                else -> throw IllegalArgumentException("Analyzer not found")
            }
        } else {
            if (config.hasValue("t0") || config.hasMeta("t0")) {
                timeAnalyzer
            } else {
                simpleAnalyzer
            }
        }
    }

    override fun analyze(block: NumassBlock, config: Meta): Values {
        val analyzer = getAnalyzer(config)
        val map = analyzer.analyze(block, config).asMap()
        map.putIfAbsent(TimeAnalyzer.T0_KEY, Value.of(0.0))
        return ValueMap(map)
    }

    override fun getEvents(block: NumassBlock, config: Meta): Stream<NumassEvent> {
        return getAnalyzer(config).getEvents(block, config)
    }

    override fun getTableFormat(config: Meta): TableFormat {
        return if (config.hasValue(TimeAnalyzer.T0_KEY) || config.hasMeta(TimeAnalyzer.T0_KEY)) {
            timeAnalyzer.getTableFormat(config)
        } else super.getTableFormat(config)
    }

}
