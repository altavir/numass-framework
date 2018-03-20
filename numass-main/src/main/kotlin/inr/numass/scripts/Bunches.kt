package inr.numass.scripts

import hep.dataforge.fx.plots.FXPlotManager
import hep.dataforge.kodex.buildMeta
import inr.numass.actions.TimeAnalyzerAction
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.buildBunchChain
import inr.numass.data.generateBlock
import inr.numass.data.generateEvents
import inr.numass.data.mergeEventChains
import java.time.Instant

fun main(args: Array<String>) {

    FXPlotManager().startGlobal()

    val cr = 10.0
    val length = 1e12.toLong()
    val num = 60;

    val blocks = (1..num).map {
        val regularChain = generateEvents(cr)
        val bunchChain = buildBunchChain(40.0, 0.01, 5.0)

        val generator = mergeEventChains(regularChain, bunchChain)
        generateBlock(Instant.now().plusNanos(it * length), length, generator)
    }

    val point = SimpleNumassPoint(10000.0, blocks)

    val meta = buildMeta {
        "t0" to 1e7
        "t0Step" to 4e6
        "normalize" to false
        "t0.crFraction" to 0.5
    }

    println("actual count rate: ${point.events.count().toDouble() / point.length.seconds}")

    TimeAnalyzerAction().simpleRun(point,meta)

//    val res = SmartAnalyzer().analyze(point, meta)
//            .getDouble(NumassAnalyzer.COUNT_RATE_KEY)
//
//    println("estimated count rate: $res")

}