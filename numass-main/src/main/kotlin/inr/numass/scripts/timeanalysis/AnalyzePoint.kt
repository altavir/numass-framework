package inr.numass.scripts.timeanalysis

import hep.dataforge.buildContext
import hep.dataforge.data.DataSet
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import inr.numass.NumassPlugin
import inr.numass.actions.TimeAnalyzerAction
import inr.numass.data.NumassDataUtils
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassDirectory

fun main() {

    val context = buildContext("NUMASS", NumassPlugin::class.java, JFreeChartPlugin::class.java) {
        output = FXOutputManager()
        rootDir = "D:\\Work\\Numass\\sterile2017_05_frames"
        dataDir = "D:\\Work\\Numass\\data\\2017_05_frames"
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
                "lo" to 1500
                "up" to 7000
            }
        }

        //"plot.showErrors" to false
    }

    //def sets = ((2..14) + (22..31)).collect { "set_$it" }
    val sets = (11..11).map { "set_$it" }
    //def sets = (16..31).collect { "set_$it" }
    //def sets = (20..28).collect { "set_$it" }

    val loaders = sets.map { set ->
        storage.provide(set, NumassSet::class.java).orElse(null)
    }.filter { it != null }

    val all = NumassDataUtils.join("sum", loaders)

    val hvs = listOf(14000.0)//, 15000d, 15200d, 15400d, 15600d, 15800d]
    //listOf(18500.0, 18600.0, 18995.0, 19000.0)

    val data = DataSet.edit(NumassPoint::class).apply {
        hvs.forEach { hv ->
            val points = all.points.filter {
                it.voltage == hv && it.channel == 0
            }.toList()
            if (!points.isEmpty()) {
                putStatic("point_${hv.toInt()}", SimpleNumassPoint.build(points, hv))
            }
        }
    }.build()


    val result = TimeAnalyzerAction.run(context, data, meta);

    result.nodeGoal().run()

    readLine()
    println("Canceling task")
    result.nodeGoal().cancel()
}