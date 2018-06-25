package inr.numass.scripts

import hep.dataforge.meta.buildMeta
import inr.numass.actions.TimeAnalyzerAction
import inr.numass.data.NumassGenerator
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.generateBlock
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import java.time.Instant

fun main(args: Array<String>) {
    val cr = 10.0
    val length = 1e12.toLong()
    val num = 60;

    val start = Instant.now()

    val blockchannel = produce {
        (1..num).forEach {
            val regularChain = NumassGenerator.generateEvents(cr)
            val bunchChain = NumassGenerator.generateBunches(40.0, 0.01, 5.0)

            send(NumassGenerator.mergeEventChains(regularChain, bunchChain).generateBlock(start.plusNanos(it * length), length))
        }
    }

    val blocks = runBlocking {
        blockchannel.toList()
    }


    val point = SimpleNumassPoint(blocks, 10000.0)

    val meta = buildMeta {
        "t0" to 1e7
        "t0Step" to 4e6
        "normalize" to false
        "t0.crFraction" to 0.5
    }

    println("actual count rate: ${point.events.count().toDouble() / point.length.seconds}")

    TimeAnalyzerAction.simpleRun(point, meta)

//    val res = SmartAnalyzer().analyze(point, meta)
//            .getDouble(NumassAnalyzer.COUNT_RATE_KEY)
//
//    println("estimated count rate: $res")

}