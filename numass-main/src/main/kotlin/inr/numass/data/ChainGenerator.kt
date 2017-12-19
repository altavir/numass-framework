package inr.numass.data

import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.SimpleBlock
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

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


private fun RandomGenerator.nextExp(mean: Double): Double {
    return -mean * Math.log(1 - nextDouble())
}

private fun RandomGenerator.nextDeltaTime(cr: Double): Long {
    return (nextExp(1.0 / cr) * 1e9).toLong()
}

class SimpleChainGenerator(
        val cr: Double,
        private val rnd: RandomGenerator = JDKRandomGenerator(),
        private val amp: RandomGenerator.(NumassEvent?, Long) -> Short = { _, _ -> ((nextDouble() + 2.0) * 100).toShort() }
) : ChainGenerator {

    override fun next(event: NumassEvent?): NumassEvent {
        return if (event == null) {
            NumassEvent(rnd.amp(null, 0), Instant.EPOCH, 0)
        } else {
            val deltaT = rnd.nextDeltaTime(cr)
            NumassEvent(rnd.amp(event, deltaT), event.blockTime, event.timeOffset + deltaT)
        }
    }
}

class BunchGenerator(
        private val cr: Double,
        private val bunchRate: Double,
        private val bunchLength: RandomGenerator.() -> Long,
        private val rnd: RandomGenerator = JDKRandomGenerator(),
        private val amp: RandomGenerator.(NumassEvent?, Long) -> Short = { _, _ -> ((nextDouble() + 2.0) * 100).toShort() }
) : ChainGenerator {

    private val internalGenerator = SimpleChainGenerator(cr, rnd, amp)

    var bunchStart: Long = 0
    var bunchEnd: Long = 0

    override fun next(event: NumassEvent?): NumassEvent {
        if (event?.timeOffset ?: 0 >= bunchEnd) {
            bunchStart = bunchEnd + rnd.nextExp(bunchRate).toLong()
            bunchEnd = bunchStart + rnd.bunchLength()
            return NumassEvent(rnd.amp(null, 0), Instant.EPOCH, bunchStart)
        } else {
            return internalGenerator.next(event)
        }
    }

    override fun generateBlock(start: Instant, length: Long, filter: (NumassEvent, NumassEvent) -> Boolean): NumassBlock {
        bunchStart = 0
        bunchEnd = 0
        return super.generateBlock(start, length, filter)
    }
}


class MergingGenerator(private  vararg val generators: ChainGenerator) : ChainGenerator {

    private val waiting: TreeSet<Pair<ChainGenerator, NumassEvent>> = TreeSet(Comparator.comparing<Pair<ChainGenerator, NumassEvent>, Long> { it.second.timeOffset })

    init {
        generators.forEach { generator ->
            waiting.add(Pair(generator, generator.next(null)))
        }
    }

    override fun next(event: NumassEvent?): NumassEvent {
        val pair = waiting.first()
        waiting.remove(pair)
        waiting.add(Pair(pair.first, pair.first.next(pair.second)))
        return pair.second
    }

    override fun generateBlock(start: Instant, length: Long, filter: (NumassEvent, NumassEvent) -> Boolean): NumassBlock {
        generators.forEach { generator ->
            waiting.add(Pair(generator, generator.next(null)))
        }
        return super.generateBlock(start, length, filter)
    }
}

