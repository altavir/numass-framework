package inr.numass.scripts.times

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.tables.ValueMap
import inr.numass.NumassPlugin
import inr.numass.data.PointAnalyzer
import inr.numass.data.RawNMPoint
import inr.numass.storage.NumassStorage
import inr.numass.storage.NumassStorageFactory

/**
 * Created by darksnake on 27-Jun-17.
 */


Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)

GrindShell shell = new GrindShell(ctx)

shell.eval {
    PlotHelper plot = plots
    File rootDir = new File("D:\\Work\\Numass\\data\\2017_05\\Fill_1")

    NumassStorage storage = NumassStorageFactory.buildLocal(rootDir);

    def set = "set_1"
    def hv = 18400;

    def loChannel = 400;
    def upChannel = 2000;

    def point = storage.provide("loader::$set/rawPoint::$hv", RawNMPoint.class).get();

    def histogram = PointAnalyzer.histogram(point, loChannel, upChannel).asTable();

    println "finished histogram calculation..."

    plot.configure("histogram") {
        yAxis(type: "log")
    }

    plot.plot(name: hv, frame: "histogram", showLine: true, showSymbol: false, showErrors: false, connectionType: "step", histogram, {
        adapter("x.value": "x", "y.value": "count")
    })

    def trueCR = PointAnalyzer.analyzePoint(point, 30e-6, loChannel, upChannel).cr

    println "The expected count rate for 30 us delay is $trueCR"

    def t0 = (1..150).collect { 5.5e-6 + 2e-7 * it }

    def statPlotPoints = t0.collect {
        def result = PointAnalyzer.analyzePoint(point, it, loChannel, upChannel)
        ValueMap.fromMap("x.value": it, "y.value": result.cr, "y.err": result.crErr);
    }
    //def cr = t0.collect { PointAnalyzer.analyzePoint(point, it).cr }

    plot.plot(name: hv, frame: "stat-method", statPlotPoints)

    def delta = 5e-6
    def discrepancyPlotPoints = (1..20).collect { delta * it }.collect {
        def t1 = it
        def t2 = it + delta
        def result = PointAnalyzer.count(point, t1, t2, loChannel, upChannel) - (Math.exp(- trueCR * t1) - Math.exp(- trueCR * t2)) * point.length * trueCR
        ValueMap.fromMap("x.value": it + delta / 2, "y.value": result);
    }

    plot.plot(name: hv, frame: "discrepancy", discrepancyPlotPoints)

//    plot.plot(title: "dead time", from: 5.5e-6, to: 2e-5) { point.cr * 1d / (1d - 6.55e-6 * point.cr) }


    storage.close()
}