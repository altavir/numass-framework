package inr.numass.data

import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.SimpleBlock
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import java.time.Duration
import java.time.Instant
import java.util.*

interface ChainGenerator {

    fun next(event: NumassEvent?): NumassEvent

    fun generateBlock(start: Instant, length: Long, filter: (NumassEvent, NumassEvent) -> Boolean = { _, _ -> true }): NumassBlock {
        val events = ArrayList<NumassEvent>()
        var event = next(null)
        events.add(event)
        while (event.timeOffset < length) {
            val nextEvent = next(event)
            if (filter(event, nextEvent)) {
                event = nextEvent
                if (event.timeOffset < length) {
                    events.add(event)
                }
            }
        }
        return SimpleBlock(start, Duration.ofNanos(length), events)
    }
}

class SimpleChainGenerator(
        val cr: Double,
        private val rnd: RandomGenerator = JDKRandomGenerator(),
        private val amp: (Long) -> Short = { 1 }
) : ChainGenerator {

    override fun next(event: NumassEvent?): NumassEvent {
        return if (event == null) {
            NumassEvent(amp(0), Instant.EPOCH, 0)
        } else {
            val deltaT = generateDeltaTime()
            NumassEvent(amp(deltaT), event.blockTime, event.timeOffset + deltaT)
        }
    }

    private fun nextExpDecay(mean: Double): Double {
        return -mean * Math.log(1 - rnd.nextDouble())
    }

    private fun generateDeltaTime(): Long {
        return (nextExpDecay(1.0 / cr) * 1e9).toLong()
    }

}
