package inr.numass.scripts.timeanalysis

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.kodex.coroutineContext
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import inr.numass.NumassPlugin
import inr.numass.actions.TimeAnalyzerAction
import inr.numass.data.NumassGenerator
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.generateBlock
import inr.numass.data.withDeadTime
import java.time.Instant

fun main(args: Array<String>) {
    Global.output = FXOutputManager()
    JFreeChartPlugin().startGlobal()
    NumassPlugin().startGlobal()

    val cr = 30e3
    val length = 30e9.toLong()
    val num = 2
    val dt = 6.5

    val start = Instant.now()

    val point = (1..num).map {
        Global.generate {
            NumassGenerator.generateEvents(cr).withDeadTime { (dt*1000).toLong() }.generateBlock(start.plusNanos(it * length), length)
        }
    }.join(Global.coroutineContext) { blocks ->
        SimpleNumassPoint(blocks, 12000.0)
    }.get()


    val meta = buildMeta {
        "analyzer" to {
            "t0" to 3000
            "chunkSize" to 5000
            "mean" to TimeAnalyzer.AveragingMethod.ARITHMETIC
        }
        "binNum" to 200
        "t0.max" to 1e4
    }

    TimeAnalyzerAction().simpleRun(point, meta);
}