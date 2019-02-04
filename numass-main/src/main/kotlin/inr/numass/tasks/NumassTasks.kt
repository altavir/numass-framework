package inr.numass.tasks

import hep.dataforge.configure
import hep.dataforge.data.CustomDataFilter
import hep.dataforge.data.DataSet
import hep.dataforge.data.DataTree
import hep.dataforge.data.DataUtils
import hep.dataforge.io.output.stream
import hep.dataforge.io.render
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import hep.dataforge.meta.buildMeta
import hep.dataforge.nullable
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.data.XYFunctionPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.plots.output.PlotOutput
import hep.dataforge.plots.output.plot
import hep.dataforge.plots.output.plotFrame
import hep.dataforge.stat.fit.FitHelper
import hep.dataforge.stat.fit.FitResult
import hep.dataforge.stat.models.XYModel
import hep.dataforge.tables.*
import hep.dataforge.useMeta
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import hep.dataforge.values.asValue
import hep.dataforge.workspace.tasks.task
import inr.numass.NumassUtils
import inr.numass.actions.MergeDataAction
import inr.numass.actions.MergeDataAction.MERGE_NAME
import inr.numass.actions.TransformDataAction
import inr.numass.addSetMarkers
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.NumassAnalyzer.Companion.CHANNEL_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_KEY
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.analyzers.countInWindow
import inr.numass.data.api.MetaBlock
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.subtractSpectrum
import inr.numass.unbox
import inr.numass.utils.ExpressionUtils
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Predicate
import java.util.stream.StreamSupport
import kotlin.collections.HashMap
import kotlin.collections.set


val selectTask = task("select") {
    descriptor {
        info = "Select data from initial data pool"
    }
    model { meta ->
        data("*")
        configure(meta.getMetaOrEmpty("data"))
    }
    transform<NumassSet> { data ->
        logger.info("Starting selection from data node with size ${data.size}")
        CustomDataFilter(meta).filter(data.checked(NumassSet::class.java)).also {
            logger.info("Selected ${it.size} elements")
        }
    }
}

val analyzeTask = task("analyze") {
    descriptor {
        info = "Count the number of events for each voltage and produce a table with the results"
    }
    model { meta ->
        dependsOn(selectTask, meta);
        configure(MetaUtils.optEither(meta, "analyzer", "prepare").orElse(Meta.empty()))
    }
    pipe<NumassSet, Table> { set ->
        SmartAnalyzer().analyzeSet(set, meta).also { res ->
            val outputMeta = meta.builder.putNode("data", set.meta)
            context.output.render(res, stage = "numass.analyze", name = name, meta = outputMeta)
        }
    }
}

val monitorTableTask = task("monitor") {
    descriptor {
        value("showPlot", types = listOf(ValueType.BOOLEAN), info = "Show plot after complete")
        value("monitorVoltage", types = listOf(ValueType.NUMBER), info = "The voltage for monitor point")
    }
    model { meta ->
        dependsOn(selectTask, meta)
        configure(meta.getMetaOrEmpty("monitor"))
        configure {
            meta.useMeta("analyzer") { putNode(it) }
        }
    }
    join<NumassSet, Table> { data ->
        val monitorVoltage = meta.getDouble("monitorVoltage", 16000.0);
        val analyzer = SmartAnalyzer()
        val analyzerMeta = meta.getMetaOrEmpty("analyzer")
        //TODO add separator labels
        val res = ListTable.Builder("timestamp", "count", "cr", "crErr")
            .rows(
                data.values.stream().parallel()
                    .flatMap { it.points.stream() }
                    .filter { it.voltage == monitorVoltage }
                    .map { it -> analyzer.analyzeParent(it, analyzerMeta) }
            ).build()

        if (meta.getBoolean("showPlot", true)) {
            val plot = DataPlot.plot(name, res, Adapters.buildXYAdapter("timestamp", "cr", "crErr"))
            context.plot(plot, name, "numass.monitor") {
                "xAxis.title" to "time"
                "xAxis.type" to "time"
                "yAxis.title" to "Count rate"
                "yAxis.units" to "Hz"
            }
            ((context.output["numass.monitor", name] as? PlotOutput)?.frame as? JFreeChartFrame)?.addSetMarkers(data.values)
        }

        context.output["numass.monitor", name].render(NumassUtils.wrap(res, meta))

        return@join res;
    }
}

val mergeTask = task("merge") {
    model { meta ->
        dependsOn(analyzeTask, meta)
        configure(meta.getMetaOrEmpty("merge"))
    }
    action(MergeDataAction)
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
            .setValue("merge.$MERGE_NAME", meta.getString("merge.$MERGE_NAME", "") + "_empty");
        dependsOn(mergeTask, newMeta)
    }
    transform<Table> { data ->
        val builder = DataSet.edit(Table::class)
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
    transform<Table> { data ->
        val builder = DataTree.edit(Table::class)
        val rootNode = data.getCheckedNode("data", Table::class.java)
        val empty = data.getCheckedNode("empty", Table::class.java).data
            ?: throw RuntimeException("No empty data found")

        rootNode.visit(Table::class.java) { input ->
            val resMeta = buildMeta {
                putNode("data", input.meta)
                putNode("empty", empty.meta)
            }
            val res = DataUtils.combine(context, input, empty, Table::class.java, resMeta) { mergeData, emptyData ->
                val dif = subtractSpectrum(mergeData, emptyData, context.logger)
                context.output["numass.merge", input.name + "_subtract"].render(NumassUtils.wrap(dif, resMeta))
                dif
            }

            builder.putData(input.name, res)
        }
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
    }
    action<Table, Table>(TransformDataAction)
}

val filterTask = task("filter") {
    model { meta ->
        dependsOn(transformTask, meta)
    }
    pipe<Table, Table> { data ->
        if (meta.hasValue("from") || meta.hasValue("to")) {
            val uLo = meta.getDouble("from", 0.0)
            val uHi = meta.getDouble("to", java.lang.Double.POSITIVE_INFINITY)
            this.log.report("Filtering finished")
            Tables.filter(data, NumassPoint.HV_KEY, uLo, uHi)
        } else if (meta.hasValue("condition")) {
            Tables.filter(data, Predicate { ExpressionUtils.condition(meta.getString("condition"), it.unbox()) })
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
        context.output["numass.fit", name].stream.use { out ->
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
                        writer.println()
                        log.entries.forEach { entry -> writer.println(entry.toString()) }
                        writer.println()
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
    pipe<FitResult, FitResult> { input ->
        val fitModel = input.optModel(context).orElseThrow { IllegalStateException("Can't load model") } as XYModel

        val data = input.data
        val adapter: ValuesAdapter = fitModel.adapter
        val function = { x: Double -> fitModel.spectrum.value(x, input.parameters) }

        val fit = XYFunctionPlot("fit", function = function).apply {
            density = 100
        }

        // ensuring all data points are calculated explicitly
        StreamSupport.stream<Values>(data.spliterator(), false)
            .map { dp -> Adapters.getXValue(adapter, dp).double }.sorted().forEach { fit.calculateIn(it) }

        val dataPlot = DataPlot.plot("data", data, adapter)

        context.plot(listOf(fit, dataPlot), name, "numass.plotFit")

        return@pipe input;
    }
}

val histogramTask = task("histogram") {
    descriptor {
        value("plot", types = listOf(ValueType.BOOLEAN), defaultValue = false, info = "Show plot of the spectra")
        value(
            "points",
            multiple = true,
            types = listOf(ValueType.NUMBER),
            info = "The list of point voltages to build histogram"
        )
        value(
            "binning",
            types = listOf(ValueType.NUMBER),
            defaultValue = 20,
            info = "The binning of resulting histogram"
        )
        value(
            "normalize",
            types = listOf(ValueType.BOOLEAN),
            defaultValue = true,
            info = "If true reports the count rate in each bin, otherwise total count"
        )
        info = "Combine amplitude spectra from multiple sets, but with the same U"
    }
    model { meta ->
        dependsOn(selectTask, meta)
        configure(meta.getMetaOrEmpty("histogram"))
        configure {
            meta.useMeta("analyzer") { putNode(it) }
            setValue("@target", meta.getString("@target", meta.name))
        }
    }
    join<NumassSet, Table> { data ->
        val analyzer = SmartAnalyzer()
        val points = meta.optValue("points").nullable?.list?.map { it.double }

        val aggregator: MutableMap<Int, MutableMap<Double, AtomicLong>> = HashMap()
        val names: SortedSet<String> = TreeSet<String>().also { it.add("channel") }

        log.report("Filling histogram")

        //Fill values to table
        data.flatMap { it.value.points }
            .filter { points == null || points.contains(it.voltage) }
            .groupBy { it.voltage }
            .mapValues {
                analyzer.getAmplitudeSpectrum(MetaBlock(it.value), meta.getMetaOrEmpty("analyzer"))
            }
            .forEach { u, spectrum ->
                log.report("Aggregating data from U = $u")
                spectrum.forEach {
                    val channel = it[CHANNEL_KEY].int
                    val count = it[COUNT_KEY].long
                    aggregator.getOrPut(channel) { HashMap() }
                        .getOrPut(u) { AtomicLong() }
                        .addAndGet(count)
                }
                names.add("U$u")
            }

        val times: Map<Double, Double> = data.flatMap { it.value.points }
            .filter { points == null || points.contains(it.voltage) }
            .groupBy { it.voltage }
            .mapValues {
                it.value.sumByDouble { it.length.toMillis().toDouble() / 1000 }
            }

        val normalize = meta.getBoolean("normalize", true)

        log.report("Combining spectra")
        val format = MetaTableFormat.forNames(names)
        val table = buildTable(format) {
            aggregator.forEach { channel, counters ->
                val values: MutableMap<String, Any> = HashMap()
                values[NumassAnalyzer.CHANNEL_KEY] = channel
                counters.forEach { u, counter ->
                    if (normalize) {
                        values["U$u"] = counter.get().toDouble() / times.getValue(u)
                    } else {
                        values["U$u"] = counter.get()
                    }
                }
                format.names.forEach {
                    values.putIfAbsent(it, 0)
                }
                row(values)
            }
        }.sumByStep(NumassAnalyzer.CHANNEL_KEY, meta.getDouble("binning", 16.0))        //apply binning

        // send raw table to the output
        context.output.render(table, stage = "numass.histogram", name = name, meta = meta)

        if (meta.getBoolean("plot", false)) {
            context.plotFrame("$name.plot", stage = "numass.histogram") {
                plots.setType<DataPlot>()
                plots.configure {
                    "showSymbol" to false
                    "showErrors" to false
                    "showLine" to true
                    "connectionType" to "step"
                }
                table.format.names.filter { it != "channel" }.forEach {
                    +DataPlot.plot(it, table, adapter = Adapters.buildXYAdapter("channel", it))
                }
            }
        }


        return@join table
    }
}

val sliceTask = task("slice") {
    model { meta ->
        dependsOn(selectTask, meta)
        configure(meta.getMetaOrEmpty("slice"))
        configure {
            meta.useMeta("analyzer") { putNode(it) }
            setValue("@target", meta.getString("@target", meta.name))
        }
    }
    join<NumassSet, Table> { data ->
        val analyzer = SmartAnalyzer()
        val slices = HashMap<String, IntRange>()
        val formatBuilder = TableFormatBuilder()
        formatBuilder.addColumn("set",ValueType.STRING)
        formatBuilder.addColumn("time",ValueType.TIME)
        meta.getMetaList("range").forEach {
            val range = IntRange(it.getInt("from"), it.getInt("to"))
            val name = it.getString("name", range.toString())
            slices[name] = range
            formatBuilder.addColumn(name, ValueType.NUMBER)
        }

        val table = buildTable(formatBuilder.build()) {
            data.forEach { setName, set ->
                val point = set.find {
                    it.index == meta.getInt("index", -1) ||
                            it.voltage == meta.getDouble("voltage", -1.0)
                }

                if (point != null) {
                    val amplitudeSpectrum = analyzer.getAmplitudeSpectrum(point, meta.getMetaOrEmpty("analyzer"))
                    val map = HashMap<String, Any>()
                    map["set"] = setName
                    map["time"] = point.startTime
                    slices.mapValuesTo(map) { (_, range) ->
                        amplitudeSpectrum.countInWindow(
                            range.start.toShort(),
                            range.endInclusive.toShort()
                        )
                    }

                    row(map)
                }
            }
        }
        // send raw table to the output
        context.output.render(table, stage = "numass.table", name = name, meta = meta)

        return@join table
    }
}

val fitScanTask = task("fitscan") {
    model { meta ->
        dependsOn(filterTask, meta)
        configure {
            setNode(meta.getMetaOrEmpty("scan"))
            setNode(meta.getMeta("fit"))
        }
    }

    splitAction<Table, FitResult> {
        val scanValues = if (meta.hasValue("scan.masses")) {
            meta.getValue("scan.masses").list.map { it -> Math.pow(it.double * 1000, 2.0).asValue() }
        } else {
            meta.getValue("scan.values", listOf(2.5e5, 1e6, 2.25e6, 4e6, 6.25e6, 9e6)).list
        }

        val scanParameter = meta.getString("parameter", "msterile2")
        scanValues.forEach { scanValue ->
            val resultName = String.format("%s[%s=%s]", this.name, scanParameter, scanValue.string)
            val fitMeta = meta.getMeta("fit").builder.apply {
                setValue("@nameSuffix", String.format("[%s=%s]", scanParameter, scanValue.string))
                if (hasMeta("params.$scanParameter")) {
                    setValue("params.$scanParameter.value", scanValue)
                } else {
                    getMetaList("params.param").stream()
                        .filter { par -> par.getString("name") == scanParameter }
                        .forEach { it.setValue("value", it) }
                }
            }

            fragment(resultName) {
                result { data ->
                    context.output["numass.fitscan", name].stream.use { out ->
                        val writer = PrintWriter(out)
                        writer.printf("%n*** META ***%n")
                        writer.println(fitMeta.toString())
                        writer.flush()

                        FitHelper(context).fit(data, fitMeta)
                            .setListenerStream(out)
                            .report(log)
                            .run()
                            .also {
                                if (fitMeta.getBoolean("printLog", true)) {
                                    writer.println()
                                    log.entries.forEach { entry -> writer.println(entry.toString()) }
                                    writer.println()
                                }
                            }
                    }
                }
            }
        }
    }
}
