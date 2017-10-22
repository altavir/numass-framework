package inr.numass.tasks

import hep.dataforge.data.CustomDataFilter
import hep.dataforge.kodex.configure
import hep.dataforge.kodex.fx.plots.PlotManager
import hep.dataforge.kodex.fx.plots.plus
import hep.dataforge.kodex.task
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.XYAdapter
import inr.numass.NumassUtils
import inr.numass.addSetMarkers
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.NumassSet

val selectDataTask = task("select") {
    model { meta ->
        data("*")
        configure(meta.getMetaOrEmpty("data"))
    }
    transform { data ->
        CustomDataFilter(meta).filter<NumassSet>(data.checked(NumassSet::class.java))
    }
}

val monitorTableTask = task("monitor") {
    model { meta ->
        dependsOn("select", meta)
        configure(meta.getMetaOrEmpty("analyzer"))
    }
    join<NumassSet, Table> {
        result { data ->
            val monitorVoltage = meta.getDouble("monitorVoltage", 16000.0);
            val analyzer = SmartAnalyzer()
            val analyzerMeta = meta.getMetaOrEmpty("analyzer")
            //TODO add separator labels
            val res = ListTable.Builder("timestamp", "count", "cr", "crErr")
                    .rows(
                            data.values.stream().parallel()
                                    .flatMap { it.points }
                                    .filter { it.voltage == monitorVoltage }
                                    .map { it -> analyzer.analyzePoint(it, analyzerMeta) }
                    ).build()

            context.provide("plots", PlotManager::class.java).ifPresent {
                it.display(stage = "monitor") {
                    configure {
                        "xAxis.title" to "time"
                        "xAxis.type" to "time"
                        "yAxis.title" to "Count rate"
                        "yAxis.units" to "Hz"
                    }
                    plots + DataPlot.plot(name, XYAdapter("timestamp", "cr", "crErr"), res)
                }.also { frame ->
                    if (frame is JFreeChartFrame) {
                        //add set markers
                        addSetMarkers(frame, data.values)
                    }
                    context.io().out("numass.monitor", name, "dfp").use {
                        NumassUtils.writeEnvelope(it, PlotFrame.Wrapper().wrap(frame))
                    }
                }
            }

            context.io().out("numass.monitor", name).use {
                NumassUtils.write(it, meta, res)
            }

            return@result res;
        }
    }
}