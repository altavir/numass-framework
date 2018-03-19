package inr.numass.scripts.times

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.GrindShell
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.maths.histogram.SimpleHistogram
import hep.dataforge.meta.Meta
import inr.numass.NumassPlugin
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassStorage
import inr.numass.data.storage.NumassStorageFactory

Context ctx = Global.instance()
ctx.getPluginManager().load(FXPlotManager)
ctx.getPluginManager().load(NumassPlugin)

new GrindShell(ctx).eval {
    File rootDir = new File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    NumassStorage storage = NumassStorageFactory.buildLocal(rootDir);

    def set = "set_3"
    def hv = 14000
    def loader = storage.provide("loader::$set", NumassSet.class).get();
    def point = loader.optPoint(hv).get()

    def table = new SimpleHistogram([0d, 0d] as Double[], [2d, 100d] as Double[])
            .fill(new TimeAnalyzer().getEventsWithDelay(point, Meta.empty()).map {
        [it.value / 1000, it.key.amp] as Double[]
    }).asTable()

    ColumnedDataWriter.writeTable(System.out, table, "hist")

    storage.close()
}