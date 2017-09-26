package inr.numass.scripts.times

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.DataSet
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindShell
import hep.dataforge.meta.Meta
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.storage.commons.StorageUtils
import inr.numass.NumassPlugin
import inr.numass.actions.TimeAnalyzedAction
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
    File rootDir = new File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    NumassStorage storage = NumassStorageFactory.buildLocal(rootDir);

    Meta meta = Grind.buildMeta(binNum: 200) {
        window(lo: 500, up: 1800)
    }

//    def set = "set_43"
//    def loader = storage.provide("loader::$set", NumassSet.class).get();
//    def data = NumassUtils.pointsToNode(loader).filter { name, data ->
//        return data.meta().getDouble("voltage",0) < 15000
//    };


    def hv = 14000;
    def dataBuilder = DataSet.builder(NumassPoint)

    StorageUtils.loaderStream(storage, false)
                .filter { it.value instanceof NumassSet }
                .forEach { pair ->
        (pair.value as NumassSet).optPoint(hv).ifPresent {
            dataBuilder.putData(pair.key, it, it.meta);
        }
    }
    def data = dataBuilder.build()

    def result = new TimeAnalyzedAction().run(ctx, data, meta);

    result.computeAll();

    storage.close()
}