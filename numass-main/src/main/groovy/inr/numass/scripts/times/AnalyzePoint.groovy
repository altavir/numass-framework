package inr.numass.scripts.times

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.DataSet
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindShell
import hep.dataforge.kodex.fx.plots.PlotManager
import hep.dataforge.meta.Meta
import inr.numass.NumassPlugin
import inr.numass.actions.TimeAnalyzerAction
import inr.numass.data.NumassDataUtils
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassStorage
import inr.numass.data.storage.NumassStorageFactory

/**
 * Created by darksnake on 27-Jun-17.
 */


Context ctx = Global.instance()
ctx.pluginManager().load(PlotManager)
ctx.pluginManager().load(NumassPlugin.class)

new GrindShell(ctx).eval {
    File rootDir = new File("D:\\Work\\Numass\\data\\2017_05\\Fill_3")

    NumassStorage storage = NumassStorageFactory.buildLocal(rootDir);

    Meta meta = Grind.buildMeta(binNum: 200) {
        window(lo: 500, up: 1800)
        plot(showErrors: false)
    }

    //def sets = ((2..14) + (22..31)).collect { "set_$it" }
    def sets = (2..14).collect { "set_$it" }
    //def sets = (16..31).collect { "set_$it" }
    //def sets = (20..28).collect { "set_$it" }

    def loaders = sets.collect { set ->
        storage.provide("loader::$set", NumassSet.class).orElse(null)
    }.findAll { it != null }

    def hvs = [14000d, 14200d, 14600d, 14800d, 15000d, 15200d, 15400d, 15600d, 15800d, 16000d]

    def all = NumassDataUtils.join("sum", loaders)

    def builder = DataSet.builder(NumassPoint)

    hvs.each { hv ->
        builder.putStatic("point_${hv as int}", new SimpleNumassPoint(hv, all.points.filter {
            it.voltage == hv
        }.collect()));
    }

    def data = builder.build()

//    def hv = 14000;
//    def dataBuilder = DataSet.builder(NumassPoint)
//
//    StorageUtils.loaderStream(storage, false)
//                .filter { it.value instanceof NumassSet }
//                .forEach { pair ->
//        (pair.value as NumassSet).optPoint(hv).ifPresent {
//            dataBuilder.putData(pair.key, it, it.meta);
//        }
//    }
//    def data = dataBuilder.build()

    def result = new TimeAnalyzerAction().run(ctx, data, meta);

    result.computeAll();

    storage.close()
}