package inr.numass.scripts

import hep.dataforge.kodex.buildMeta
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.buildBunchChain
import inr.numass.data.buildSimpleEventChain
import inr.numass.data.generateBlock
import inr.numass.data.mergeEventChains
import java.time.Instant

fun main(args: Array<String>) {

    val cr = 10.0
    val length = 1e12.toLong()
    val num = 20;

    val blocks = (1..num).map {
        val regularChain = buildSimpleEventChain(cr)
        val bunchChain = buildBunchChain(20.0, 0.01, 5.0)

        val generator = mergeEventChains(regularChain, bunchChain)
        generateBlock(Instant.now().plusNanos(it * length), length, generator)
    }

    val point = SimpleNumassPoint(10000.0, blocks)

    val meta = buildMeta {
        "t0.crFraction" to 0.1
    }

    println("actual count rate: ${point.events.count().toDouble() / point.length.seconds}")

    val res = SmartAnalyzer().analyze(point, meta)
            .getDouble(NumassAnalyzer.COUNT_RATE_KEY)

    println("estimated count rate: $res")

}