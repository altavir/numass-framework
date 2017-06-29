package inr.numass.scripts

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.tables.MapPoint
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

    def hv = 14000;
    def point = storage.provide("loader::set_2/rawPoint::$hv", RawNMPoint.class).get();

    def t0 = (1..150).collect { 5.5e-6 + 2e-7 * it }

    def plotPoints = t0.collect {
        def result = PointAnalyzer.analyzePoint(point, it)
        MapPoint.fromMap("x.value": it, "y.value": result.cr, "y.err": result.crErr);
    }
    //def cr = t0.collect { PointAnalyzer.analyzePoint(point, it).cr }

    plot.plot(plotPoints, ["name": hv])


    plot.plot(title: "dead time", from: 5.5e-6, to: 2e-5) { point.cr * 1d / (1d - 6.55e-6 * point.cr) }

    storage.close()
}