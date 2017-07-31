package inr.numass.scripts.times

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.tables.ValueMap
import inr.numass.NumassPlugin
import inr.numass.data.PointAnalyzer
import inr.numass.data.api.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassStorage
import inr.numass.data.storage.NumassStorageFactory

/**
 * Created by darksnake on 27-Jun-17.
 */


Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)

new GrindShell(ctx).eval {
    PlotHelper plot = plots
    File rootDir = new File("D:\\Work\\Numass\\data\\2017_05\\Fill_3")

    NumassStorage storage = NumassStorageFactory.buildLocal(rootDir);


    def set = "set_43"
    def hv = 16000;
    def loader = storage.provide("loader::$set", NumassSet.class).get();
    def point = loader.provide("$hv", NumassPoint.class).get()

    def loChannel = 500;
    def upChannel = 2000;

    def histogram = PointAnalyzer.histogram(point, loChannel, upChannel, 0.7, 1000).asTable();

    println "finished histogram calculation..."

    plot.configure("histogram") {
        xAxis(axisTitle: "delay", axisUnits: "us")
        yAxis(type: "log")
    }

    plot.plot(name: "test", frame: "histogram", showLine: true, showSymbol: false, showErrors: false, connectionType: "step", histogram, {
        adapter("x.value": "x", "y.value": "count")
    })

    def trueCR = PointAnalyzer.analyze(point, t0: 30e3, "window.lo": loChannel, "window.up": upChannel).getDouble("cr")

    println "The expected count rate for 30 us delay is $trueCR"

    def t0 = (1..150).collect { 500 * it }

    def statPlotPoints = t0.collect {
        def result = PointAnalyzer.analyze(point, t0: it, "window.lo": loChannel, "window.up": upChannel)
        ValueMap.ofMap("x": it / 1000, "y": result.getDouble("cr"), "y.err": result.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY));
    }
    plot.plot(name: "total", frame: "stat-method", showLine: true, statPlotPoints)

//    def delta = 5e-6
//    def discrepancyPlotPoints = (1..20).collect { delta * it }.collect {
//        def t1 = it
//        def t2 = it + delta
//        def result = PointAnalyzer.count(point, t1, t2, loChannel, upChannel) - (Math.exp(-trueCR * t1) - Math.exp(-trueCR * t2)) * point.length * trueCR
//        ValueMap.ofMap("x.value": it + delta / 2, "y.value": result);
//    }
//
//    plot.plot(name: hv, frame: "discrepancy", discrepancyPlotPoints)


    storage.close()
}