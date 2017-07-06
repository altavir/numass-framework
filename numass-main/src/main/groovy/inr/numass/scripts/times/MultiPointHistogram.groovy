package inr.numass.scripts.times

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.plots.fx.FXPlotManager
import inr.numass.NumassPlugin
import inr.numass.data.PointAnalyzer
import inr.numass.data.RawNMPoint
import inr.numass.storage.NumassDataLoader
import inr.numass.storage.NumassStorage
import inr.numass.storage.NumassStorageFactory

/**
 * Created by darksnake on 06-Jul-17.
 */


Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)

new GrindShell(ctx).eval {
    PlotHelper plot = plots
    File rootDir = new File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    NumassStorage storage = NumassStorageFactory.buildLocal(rootDir);

    def pattern = "set_.{1,2}"

    List<NumassDataLoader> loaders = storage.loaders().findAll{it.name.matches(pattern)}.collect{it as NumassDataLoader}

    println "Found ${loaders.size()} loaders matching pattern"

    def hv = 16000.toString();
    List<RawNMPoint> points = loaders.collect { loader -> loader.optRawPoint(hv).get()}

    def loChannel = 400;
    def upChannel = 800;

    def chain = PointAnalyzer.timeChain(loChannel,upChannel, points as RawNMPoint[])

    def histogram = PointAnalyzer.histogram(chain, 5e-6,500).asTable();

    println "finished histogram calculation..."

    plot.configure("histogram") {
        yAxis(type: "log")
    }

    plot.plot(name: hv, frame: "histogram", showLine: true, showSymbol: false, showErrors: false, connectionType: "step", histogram, {
        adapter("x.value": "x", "y.value": "count")
    })

    storage.close()
}