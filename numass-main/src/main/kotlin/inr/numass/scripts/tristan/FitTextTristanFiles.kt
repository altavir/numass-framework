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

package inr.numass.scripts.tristan

import hep.dataforge.buildContext
import hep.dataforge.configure
import hep.dataforge.data.DataNode
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.io.ColumnedDataReader
import hep.dataforge.io.DirectoryOutput
import hep.dataforge.io.output.stream
import hep.dataforge.io.plus
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.plots.output.plotFrame
import hep.dataforge.stat.fit.FitHelper
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.tables.*
import hep.dataforge.values.ValueType
import inr.numass.NumassPlugin
import inr.numass.actions.MergeDataAction
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_ERROR_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.LENGTH_KEY
import inr.numass.data.api.NumassPoint.Companion.HV_KEY
import java.io.PrintWriter
import java.nio.file.Files
import java.util.function.Predicate

fun main(args: Array<String>) {
    val context = buildContext("NUMASS") {
        plugin<NumassPlugin>()
        plugin<JFreeChartPlugin>()
        rootDir = "D:\\Work\\Numass\\TristanText\\"
        dataDir = "D:\\Work\\Numass\\TristanText\\data\\"
        output = FXOutputManager() + DirectoryOutput()
    }
    context.load<NumassPlugin>()


    val tables = DataNode.build<Table> {
        name = "tristan"
        Files.list(context.dataDir).forEach {
            val name = ".*(set_\\d+).*".toRegex().matchEntire(it.fileName.toString())!!.groupValues[1]
            val table = ColumnedDataReader(Files.newInputStream(it), HV_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY).toTable()
                    .addColumn(LENGTH_KEY, ValueType.NUMBER) { 30 }
                    .addColumn(COUNT_KEY, ValueType.NUMBER) { getDouble(COUNT_RATE_KEY) * 30 }
            putStatic(name, table)
        }
    }
    val adapter = Adapters.buildXYAdapter(HV_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY)

    context.plotFrame("raw", "plots") {
        configure {
            "legend.show" to false
        }
        plots.configure {
            "showLine" to true
            "showSymbol" to false
        }
        tables.forEach { (key, value) ->
            add(DataPlot.plot(key, value, adapter))
        }
    }

    context.plotFrame("raw_normalized", "plots") {
        configure {
            "legend.show" to false
        }
        plots.configure {
            "showLine" to true
            "showSymbol" to false
        }
        tables.forEach { (key, table) ->
            val norming = table.find { it.getDouble(HV_KEY) == 13000.0 }!!.getDouble(COUNT_RATE_KEY)
            val normalizedTable = table
                    .replaceColumn(COUNT_RATE_KEY) { getDouble(COUNT_RATE_KEY) / norming }
                    .replaceColumn(COUNT_RATE_ERROR_KEY) { getDouble(COUNT_RATE_ERROR_KEY) / norming }
            add(DataPlot.plot(key, normalizedTable, adapter))
        }
    }

    val merge = MergeDataAction.runGroup(context, tables, Meta.empty()).get()

    val filtered = Tables.filter(merge,
            Predicate {
                val hv = it.getDouble(HV_KEY)
                hv > 12200.0 && (hv < 15500 || hv > 16500)
            }
    )

    context.plotFrame("merge", "plots") {
        plots.configure {
            "showLine" to true
            "showSymbol" to false
        }
        add(DataPlot.plot("merge", merge, adapter))
    }

    val meta = buildMeta {
        "model" to {
            "modelName" to "sterile"
            "resolution" to {
                "width" to 8.3e-5
                "tail" to "function::numass.resolutionTail.2017.mod"
            }
            "transmission" to {
                "trapping" to "function::numass.trap.nominal"
            }
        }
        "stage" to { "freePars" to listOf("N", "bkg", "E0") }
    }

    val params = ParamSet().apply {
        setPar("N", 4e4, 6.0, 0.0, Double.POSITIVE_INFINITY)
        setPar("bkg", 2.0, 0.03)
        setPar("E0", 18575.0, 1.0)
        setPar("mnu2", 0.0, 1.0)
        setParValue("msterile2", (1000 * 1000).toDouble())
        setPar("U2", 0.0, 1e-3)
        setPar("X", 0.1, 0.01)
        setPar("trap", 1.0, 0.01)
    }

    context.output["numass.fit", "text"].stream.use { out ->
        val log = context.history.getChronicle("log")
        val writer = PrintWriter(out)
        writer.printf("%n*** META ***%n")
        writer.println(meta.toString())
        writer.flush()
        FitHelper(context)
                .fit(filtered, meta)
                .setListenerStream(out)
                .report(log)
                .apply {
                    params(params)
                }.run()

        writer.println()
        log.entries.forEach { entry -> writer.println(entry.toString()) }
        writer.println()

    }

}