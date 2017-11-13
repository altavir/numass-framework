package inr.numass.tasks

import hep.dataforge.data.CustomDataFilter
import hep.dataforge.data.DataSet
import hep.dataforge.data.DataTree
import hep.dataforge.data.DataUtils
import hep.dataforge.description.ValueDef
import hep.dataforge.fx.plots.PlotManager
import hep.dataforge.fx.plots.plus
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.configure
import hep.dataforge.kodex.task
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.PlotUtils
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.data.XYFunctionPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.stat.fit.FitHelper
import hep.dataforge.stat.fit.FitResult
import hep.dataforge.stat.models.XYModel
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import hep.dataforge.tables.XYAdapter
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import inr.numass.NumassUtils
import inr.numass.actions.MergeDataAction
import inr.numass.actions.MergeDataAction.Companion.MERGE_NAME
import inr.numass.actions.TransformDataAction
import inr.numass.addSetMarkers
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.subtract
import inr.numass.unbox
import inr.numass.utils.ExpressionUtils
import java.io.PrintWriter
import java.util.stream.StreamSupport

val selectTask = task("select") {
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
        dependsOn(selectTask, meta)
        configure(meta.getMetaOrEmpty("analyzer"))
    }
    join<NumassSet, Table> { data ->
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
                    context.getIo().out("numass.monitor", name, "dfp").use {
                        NumassUtils.writeEnvelope(it, PlotFrame.Wrapper().wrap(frame))
                    }
                }
            }
        }

        context.getIo().out("numass.monitor", name).use {
            NumassUtils.write(it, meta, res)
        }

        return@join res;
    }
}

val analyzeTask = task("analyze") {
    model { meta ->
        dependsOn(selectTask, meta);
        configure(MetaUtils.optEither(meta, "analyzer", "prepare").orElse(Meta.empty()))
    }
    pipe<NumassSet, Table> { set ->
        SmartAnalyzer().analyzeSet(set, meta).also { res ->
            context.getIo().out("numass.analyze", name).use {
                NumassUtils.write(it, meta, res)
            }
        }
    }
}

val mergeTask = task("merge") {
    model { meta ->
        dependsOn(analyzeTask, meta)
        configure(meta.getMetaOrEmpty("merge"))
    }
    action<Table, Table>(MergeDataAction())
}

//val newMergeTask = task("merge") {
//    model { meta ->
//        dependsOn(analyzeTask, meta)
//    }
//    join<Table, Table> {
//        byValue(MERGE_NAME)
//    }
//}

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
                .setValue("merge." + MERGE_NAME, meta.getString("merge." + MERGE_NAME, "") + "_empty");
        dependsOn(mergeTask, newMeta)
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
        dependsOn(mergeTask, meta, "data")
        dependsOn(mergeEmptyTask, meta, "empty")
    }
    transform<Table, Table> { data ->
        val builder = DataTree.builder(Table::class.java)
        val rootNode = data.getCheckedNode<Table>("data", Table::class.java)
        val empty = data.getCheckedNode<Table>("empty", Table::class.java).data

        rootNode.forEachData(Table::class.java, { input ->
            val resMeta = buildMeta {
                node("data", input.meta)
                node("empty", empty.meta)
            }
            val res = DataUtils.combine(input, empty, Table::class.java, resMeta) { mergeData, emptyData ->
                subtract(context, mergeData, emptyData)
            }

            res.goal.onComplete { r, _ ->
                if (r != null) {
                    context.getIo().out("numass.merge", input.name + "_subtract").use {
                        NumassUtils.write(it, resMeta, r)
                    }
                }
            }

            builder.putData(input.name, res)
        })
        builder.build()
    }
}

val transformTask = task("transform") {
    model { meta ->
        if (meta.hasMeta("merge")) {
            if (meta.hasMeta("empty")) {
                dependsOn(subtractEmptyTask, meta)
            } else {
                dependsOn(mergeTask, meta);
            }
        } else {
            dependsOn(analyzeTask, meta);
        }
        configure(MetaUtils.optEither(meta, "transform", "prepare").orElse(Meta.empty()))
    }
    action<Table, Table>(TransformDataAction());
}

val filterTask = task("filter") {
    model { meta ->
        dependsOn(transformTask, meta)
    }
    pipe<Table, Table> { data ->
        if (meta.hasValue("from") || meta.hasValue("to")) {
            val uLo = meta.getDouble("from", 0.0)!!
            val uHi = meta.getDouble("to", java.lang.Double.POSITIVE_INFINITY)!!
            this.log.report("Filtering finished")
            TableTransform.filter(data, NumassPoint.HV_KEY, uLo, uHi)
        } else if (meta.hasValue("condition")) {
            TableTransform.filter(data) { ExpressionUtils.condition(meta.getString("condition"), it.unbox()) }
        } else {
            throw RuntimeException("No filtering condition specified")
        }
    }

}

val fitTask = task("fit") {
    model { meta ->
        dependsOn(filterTask, meta)
        configure(meta.getMeta("fit"))
    }
    pipe<Table, FitResult> { data ->
        context.getIo().out("numass.fit", name).use { out ->
            val writer = PrintWriter(out)
            writer.printf("%n*** META ***%n")
            writer.println(meta.toString())
            writer.flush()

            FitHelper(context).fit(data, meta)
                    .setListenerStream(out)
                    .report(log)
                    .run()
                    .also {
                        if (meta.getBoolean("printLog", true)) {
                            log.print(writer)
                        }
                    }
        }
    }
}

val plotFitTask = task("plotFit") {
    model { meta ->
        dependsOn(fitTask, meta)
        configure(meta.getMetaOrEmpty("plotFit"))
    }
    pipe<FitResult, PlotFrame> { input ->
        val fitModel = input.optModel(context).orElseThrow { IllegalStateException("Can't load model") } as XYModel

        val data = input.data

        val adapter: XYAdapter = fitModel.adapter

        val function = { x: Double -> fitModel.spectrum.value(x, input.parameters) }

        val frame = PlotUtils.getPlotManager(context)
                .getPlotFrame("numass.plotFit", name, meta.getMeta("frame", Meta.empty()))

        val fit = XYFunctionPlot("fit").apply {
            setFunction(function)
            setDensity(100, false)
            setSmoothing(true)
        }

        frame.add(fit)

        // ensuring all data points are calculated explicitly
        StreamSupport.stream<Values>(data.spliterator(), false)
                .map { dp -> adapter.getX(dp).doubleValue() }.sorted().forEach { fit.calculateIn(it) }

        frame.add(DataPlot.plot("data", adapter, data))

        return@pipe frame;
    }
}
