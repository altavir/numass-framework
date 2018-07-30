package inr.numass.scripts.timeanalysis

import hep.dataforge.context.Global
import hep.dataforge.coroutineContext
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.goals.generate
import hep.dataforge.goals.join
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import inr.numass.NumassPlugin
import inr.numass.actions.TimeAnalyzerAction
import inr.numass.data.NumassGenerator
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.generateBlock
import inr.numass.data.withDeadTime
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.SynchronizedRandomGenerator
import java.time.Instant

fun main(args: Array<String>) {
    Global.output = FXOutputManager()
    JFreeChartPlugin().startGlobal()
    NumassPlugin().startGlobal()

    val cr = 30e3
    val length = 1e9.toLong()
    val num = 50
    val dt = 6.5

    val start = Instant.now()

    val generator = SynchronizedRandomGenerator(JDKRandomGenerator(2223))

    val point = (1..num).map {
        Global.generate {
            NumassGenerator
                    .generateEvents(cr * (1.0 - 0.005 * it), rnd = generator)
                    .withDeadTime { (dt * 1000).toLong() }
                    .generateBlock(start.plusNanos(it * length), length)
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
        "t0.max" to 5e4
    }

    TimeAnalyzerAction.simpleRun(point, meta);

    readLine()
}