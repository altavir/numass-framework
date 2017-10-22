package inr.numass.tasks

import hep.dataforge.data.CustomDataFilter
import hep.dataforge.data.DataSet
import hep.dataforge.data.DataTree
import hep.dataforge.data.DataUtils
import hep.dataforge.description.ValueDef
import hep.dataforge.io.ColumnedDataWriter
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
import hep.dataforge.values.ValueType
import inr.numass.NumassUtils
import inr.numass.actions.MergeDataAction
import inr.numass.actions.MergeDataAction.MERGE_NAME
import inr.numass.addSetMarkers
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.NumassSet
import inr.numass.subtract

val selectDataTask = task("select") {
    model { meta ->
        data("*")
        configure(meta.getMetaOrEmpty("data"))
    }
    transform<NumassSet, NumassSet> { data ->
        CustomDataFilter(meta).filter<NumassSet>(data.checked(NumassSet::class.java))
    }
}

@ValueDef(name = "showPlot", type = arrayOf(ValueType.BOOLEAN), info = "Show plot after complete")
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

            if (meta.getBoolean("showPlot", true)) {
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
            }

            context.io().out("numass.monitor", name).use {
                NumassUtils.write(it, meta, res)
            }

            return@result res;
        }
    }
}

val analyzeTask = task("analyze") {
    model { meta ->
        dependsOn("select", meta);
        configure(meta.getMetaOrEmpty("analyzer"))
    }
    pipe<NumassSet, Table> {
        result { set ->
            SmartAnalyzer().analyzeSet(set, meta).also { res ->
                context.io().out("numass.analyze", name).use {
                    NumassUtils.write(it, meta, res)
                }
            }
        }
    }
}

val mergeTask = task("merge") {
    model { meta ->
        dependsOn("analyze", meta)
        configure(meta.getMetaOrEmpty("merge"))
    }
    action<Table, Table>(MergeDataAction())
}

val mergeEmptyTask = task("empty") {
    model { meta ->
        if (!meta.hasMeta("empty")) {
            throw RuntimeException("Empty source data not found ");
        }
        //replace data node by "empty" node
        val newMeta = meta.builder
                .removeNode("data")
                .removeNode("empty")
                .setNode("data", meta.getMeta("empty"))
                .setValue(MERGE_NAME, meta.getString(MERGE_NAME, "") + "_empty");
        dependsOn("merge", newMeta)
    }
    transform<Table, Table> { data ->
        val builder = DataSet.builder(Table::class.java)
        data.forEach {
            builder.putData(it.name + "_empty", it.anonymize());
        }
        builder.build()
    }
}


val subtractEmptyTask = task("dif") {
    model { meta ->
        dependsOn("merge", meta, "data")
        dependsOn("empty", meta, "empty")
    }
    transform<Table,Table> { data ->
        val builder = DataTree.builder(Table::class.java)
        val rootNode = data.getCheckedNode<Table>("data", Table::class.java)
        val empty = data.getCheckedNode<Table>("empty", Table::class.java).data
        rootNode.forEachData(Table::class.java, { input ->
            val res = DataUtils.combine(input, empty, Table::class.java, input.meta()) { mergeData, emptyData ->
                subtract(context, mergeData, emptyData)
            }

            res.goal.onComplete { r, _ ->
                if (r != null) {
                    val out = context.io().out("numass.merge", input.name + "_subtract")
                    ColumnedDataWriter.writeTable(out, r,
                            input.meta().builder.setNode("empty", empty.meta()).toString())
                }
            }

            builder.putData(input.name, res)
        })
        builder.build()
    }
}