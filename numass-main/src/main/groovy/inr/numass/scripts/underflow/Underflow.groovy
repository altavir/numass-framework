/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts.underflow

import hep.dataforge.cache.CachePlugin
import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataSet
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.actions.GrindPipe
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.meta.Meta
import hep.dataforge.plots.data.PlottableData
import hep.dataforge.plots.data.PlottableGroup
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.storage.commons.StorageUtils
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import hep.dataforge.tables.XYAdapter
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassStorage
import inr.numass.data.storage.NumassStorageFactory

import java.util.stream.Collectors

import static hep.dataforge.grind.Grind.buildMeta
import static inr.numass.data.api.NumassAnalyzer.CHANNEL_KEY
import static inr.numass.data.api.NumassAnalyzer.COUNT_RATE_KEY

Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)
ctx.pluginManager().load(CachePlugin.class)

Meta meta = buildMeta {
    data(dir: "D:\\Work\\Numass\\data\\2017_05\\Fill_2", mask: "set_.{1,3}")
    generate(t0: 3e4, sort: false)
    subtract(reference: 18600)
    fit(xlow: 450, xHigh: 700, upper: 3100, binning: 20)
}


new GrindShell(ctx).eval {

    //Defining root directory
    File dataDirectory = new File(meta.getString("data.dir"))

    //creating storage instance

    NumassStorage storage = NumassStorageFactory.buildLocal(dataDirectory);

    //Reading points
    //Free operation. No reading done
    List<NumassSet> sets = StorageUtils
            .loaderStream(storage)
            .filter { it.key.matches(meta.getString("data.mask")) }
            .map {
        println "loading ${it.key}"
        return it.value
    }.collect(Collectors.toList());

    NumassAnalyzer analyzer = new TimeAnalyzer();

    def dataBuilder = DataSet.builder(NumassPoint);

    sets.sort { it.startTime }
        .collectMany { it.points.collect() }
        .groupBy { it.voltage }
        .each { key, value ->
        def point = new SimpleNumassPoint(key as double, value as List<NumassPoint>)
        String name = (key as Integer).toString()
        dataBuilder.putStatic(name, point, buildMeta(voltage: key));
    }

    DataNode<NumassPoint> data = dataBuilder.build()

    def generate = GrindPipe.<NumassPoint, Table> build(name: "generate") {
        return analyzer.getSpectrum(delegate.input as NumassPoint, delegate.meta)
    }

    DataNode<Table> spectra = generate.run(context, data, meta.getMeta("generate"));
    spectra = context.getFeature(CachePlugin).cacheNode("underflow", meta, spectra)

    //subtracting reference point
    Map<Double, Table> spectraMap
    if (meta.hasValue("subtract.reference")) {
        String referenceVoltage = meta["subtract.reference"].stringValue()
        println "subtracting reference point ${referenceVoltage}"
        def referencePoint = spectra.compute(referenceVoltage)
        spectraMap = spectra
                .findAll { it.name != referenceVoltage }
                .collectEntries {
                    [(it.meta["voltage"].doubleValue()): NumassDataUtils.subtractSpectrum(it.get(), referencePoint)]
                }
    } else {
        spectraMap = spectra.collectEntries { return [(it.meta["voltage"].doubleValue()): it.get()] }
    }

    //Showing selected points
    def showPoints = { Map points, int binning = 20, int loChannel = 300, int upChannel = 2000 ->
        PlottableGroup<PlottableData> plotGroup = new PlottableGroup<>();
        def adapter = new XYAdapter(CHANNEL_KEY, COUNT_RATE_KEY)
        points.each {
            plotGroup.add(
                    PlottableData.plot(
                            it.key as String,
                            adapter,
                            NumassAnalyzer.spectrumWithBinning(it.value as Table, binning)
                    )
            )
        }

        //configuring and plotting
        plotGroup.configure(showLine: true, showSymbol: false, showErrors: false, connectionType: "step")
        def frame = (plots as PlotHelper).getManager().getPlotFrame("Spectra")
        frame.configureValue("yAxis.type", "log")
        frame.addAll(plotGroup)
    }

    showPoints(spectraMap.findAll { it.key in [16200d, 16400d, 16800d, 17000d, 17200d, 17700d] })

    Table correctionTable = TableTransform.filter(
            UnderflowFitter.fitAllPoints(
                    spectraMap,
                    meta["fit.xlow"].intValue(),
                    meta["fit.xHigh"].intValue(),
                    meta["fit.upper"].intValue(),
                    meta["fit.binning"].intValue()
            ),
            "correction",
            0,
            2
    )

    ColumnedDataWriter.writeTable(System.out, correctionTable, "underflow parameters")

    (plots as PlotHelper).plot(correctionTable, name: "correction", frame: "Correction") {
        adapter("x.value": "U", "y.value": "correction")
    }
}