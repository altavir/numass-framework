package inr.numass.scripts

import hep.dataforge.fx.plots.PlotManager
import hep.dataforge.kodex.buildContext
import hep.dataforge.kodex.buildMeta
import inr.numass.NumassPlugin
import inr.numass.data.BunchGenerator
import inr.numass.data.MergingGenerator
import inr.numass.data.SimpleChainGenerator
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.SimpleNumassPoint
import java.time.Instant

fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java, PlotManager::class.java)

    val cr = 10.0
    val length = 30e9.toLong()
    val num = 50;
    val dt = 6.5

    val regularGenerator = SimpleChainGenerator(cr)
    val bunchGenerator = BunchGenerator(40.0, 0.1, { 2e9.toLong() })

    val generator = MergingGenerator(regularGenerator, bunchGenerator)

    val blocks = (1..num).map {
        generator.generateBlock(Instant.now().plusNanos(it * length), length)
    }

    val point = SimpleNumassPoint(10000.0, blocks)

    val meta = buildMeta {
        "t0.crFraction" to 0.1
    }

    println("actual count rate: ${point.events.count() / point.length.seconds}")

    val res = SmartAnalyzer().analyze(point,meta)
            .getDouble(NumassAnalyzer.COUNT_RATE_KEY)

    println("estimated count rate: ${res}")

}