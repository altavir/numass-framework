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
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableFormat
import hep.dataforge.values.Value
import hep.dataforge.values.ValueMap
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import inr.numass.data.ChernovProcessor
import inr.numass.data.api.*
import inr.numass.utils.ExpressionUtils

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
                else -> throw IllegalArgumentException("Analyzer ${config.getString("type")} not found")
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
        val map = analyzer.analyze(block, config).asMap().toMutableMap()
        map.putIfAbsent(TimeAnalyzer.T0_KEY, Value.of(0.0))
        return ValueMap(map)
    }

    override fun getEvents(block: NumassBlock, meta: Meta): List<NumassEvent> {
        return getAnalyzer(meta).getEvents(block, meta)
    }

    override fun getTableFormat(config: Meta): TableFormat {
        return if (config.hasValue(TimeAnalyzer.T0_KEY) || config.hasMeta(TimeAnalyzer.T0_KEY)) {
            timeAnalyzer.getTableFormat(config)
        } else super.getTableFormat(config)
    }

    override fun analyzeSet(set: NumassSet, config: Meta): Table {
        fun Value.computeExpression(point: NumassPoint): Int {
            return when {
                this.type == ValueType.NUMBER -> this.int
                this.type == ValueType.STRING -> {
                    val exprParams = HashMap<String, Any>()

                    exprParams["U"] = point.voltage

                    ExpressionUtils.function(this.string, exprParams).toInt()
                }
                else -> error("Can't interpret $type as expression or number")
            }
        }
        val lo = config.getValue("window.lo",0)
        val up = config.getValue("window.up", Int.MAX_VALUE)

        val format = getTableFormat(config)

        return ListTable.Builder(format)
            .rows(set.points.map { point ->
                val newConfig = config.builder.apply{
                    setValue("window.lo", lo.computeExpression(point))
                    setValue("window.up", up.computeExpression(point))
                }
                analyzeParent(point, newConfig)
            })
            .build()
    }
}