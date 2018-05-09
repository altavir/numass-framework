package inr.numass.scripts.timeanalysis

import hep.dataforge.data.DataSet
import hep.dataforge.fx.plots.FXPlotManager
import hep.dataforge.kodex.buildContext
import hep.dataforge.kodex.buildMeta
import inr.numass.NumassPlugin
import inr.numass.actions.TimeAnalyzerAction
import inr.numass.data.NumassDataUtils
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassStorageFactory

fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java, FXPlotManager::class.java) {
        rootDir = "D:\\Work\\Numass\\sterile\\2018_04"
        dataDir = "D:\\Work\\Numass\\data\\2018_04"
    }

    val storage = NumassStorageFactory.buildLocal(context, "Fill_2", true, false);

    val meta = buildMeta {
        "binNum" to 200
        "inverted" to true
        node("window") {
            "lo" to 500
            "up" to 3000
        }
        "plot.showErrors" to false
    }

    //def sets = ((2..14) + (22..31)).collect { "set_$it" }
    val sets = (2..14).map { "set_$it" }
    //def sets = (16..31).collect { "set_$it" }
    //def sets = (20..28).collect { "set_$it" }

    val loaders = sets.map { set ->
        storage.provide("loader::$set", NumassSet::class.java).orElse(null)
    }.filter { it != null }

    val hvs = listOf(14000.0, 14200.0, 14600.0, 14800.0)//, 15000d, 15200d, 15400d, 15600d, 15800d]

    val all = NumassDataUtils.join("sum", loaders)

    val data = DataSet.edit(NumassPoint::class).apply {
        hvs.forEach { hv ->
            putStatic(
                    "point_${hv.toInt()}",
                    SimpleNumassPoint(
                            all.points.filter {
                                it.voltage == hv
                            }.toList(),
                            hv
                    )
            )
        }
    }.build()


    val result = TimeAnalyzerAction().run(context, data, meta);

    result.computeAll();

}