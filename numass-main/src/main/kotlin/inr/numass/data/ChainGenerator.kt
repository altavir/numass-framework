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

    fun generateBlock(start: Instant, length: Long): NumassBlock {
        val events = ArrayList<NumassEvent>()
        var event = next(null)
        while (event.timeOffset < length) {
            events.add(event)
            event = next(event)
        }
        return SimpleBlock(start, Duration.ofNanos(length), events)
    }
}

class SimpleChainGenerator(val cr: Double, private var rnd: RandomGenerator = JDKRandomGenerator(), private val amp: () -> Short = { 1 }) : ChainGenerator {

    override fun next(event: NumassEvent?): NumassEvent {
        return if (event == null) {
            NumassEvent(amp(), Instant.EPOCH, 0)
        } else {
            NumassEvent(amp(), event.blockTime, event.timeOffset + generateDeltaTime())
        }
    }

    private fun nextExpDecay(mean: Double): Double {
        return -mean * Math.log(1 - rnd.nextDouble())
    }

    private fun generateDeltaTime(): Long {
        return (nextExpDecay(1.0 / cr) * 1e9).toLong()
    }

}
