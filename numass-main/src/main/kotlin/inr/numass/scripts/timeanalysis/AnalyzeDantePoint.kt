package inr.numass.scripts.timeanalysis

import hep.dataforge.buildContext
import hep.dataforge.data.DataSet
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import inr.numass.NumassPlugin
import inr.numass.actions.TimeAnalyzerAction
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassDirectory

fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java, JFreeChartPlugin::class.java) {
        output = FXOutputManager()
        rootDir = "D:\\Work\\Numass\\sterile2018_04"
        dataDir = "D:\\Work\\Numass\\data\\2018_04"
    }

    val storage = NumassDirectory.read(context, "Fill_3")!!

    val meta = buildMeta {
        "binNum" to 200
        //"chunkSize" to 10000
       // "mean" to TimeAnalyzer.AveragingMethod.ARITHMETIC
        //"separateParallelBlocks" to true
        "t0" to {
            "step" to 320
        }
        "analyzer" to {
            "t0" to 16000
            "window" to {
                "lo" to 450
                "up" to 1900
            }
        }

        //"plot.showErrors" to false
    }


    val loader = storage.provide("set_9",NumassSet::class.java).get()

    val hvs = listOf(14000.0)//, 15000d, 15200d, 15400d, 15600d, 15800d]
    //listOf(18500.0, 18600.0, 18995.0, 19000.0)

    val data = DataSet.edit(NumassPoint::class).apply {
        hvs.forEach { hv ->
            val points = loader.points.filter {
                it.voltage == hv
            }.map { it.channels[0]!! }.toList()
            if (!points.isEmpty()) {
                putStatic(
                        "point_${hv.toInt()}",
                        SimpleNumassPoint(points, hv)
                )
            }
        }
    }.build()


    val result = TimeAnalyzerAction.run(context, data, meta);

    result.nodeGoal().run()

    readLine()
    println("Canceling task")
    result.nodeGoal().cancel()
}