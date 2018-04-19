package inr.numass.data

import hep.dataforge.maths.chain.Chain
import hep.dataforge.maths.chain.MarkovChain
import hep.dataforge.maths.chain.StatefulChain
import hep.dataforge.stat.defaultGenerator
import hep.dataforge.tables.Table
import inr.numass.data.analyzers.NumassAnalyzer.Companion.CHANNEL_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_KEY
import inr.numass.data.api.*
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.takeWhile
import kotlinx.coroutines.experimental.channels.toList
import org.apache.commons.math3.distribution.EnumeratedRealDistribution
import org.apache.commons.math3.random.RandomGenerator
import java.time.Duration
import java.time.Instant

private fun RandomGenerator.nextExp(mean: Double): Double {
    return -mean * Math.log(1 - nextDouble())
}

private fun RandomGenerator.nextDeltaTime(cr: Double): Long {
    return (nextExp(1.0 / cr) * 1e9).toLong()
}

fun generateBlock(start: Instant, length: Long, chain: Chain<OrphanNumassEvent>): NumassBlock {
    return SimpleBlock(start, Duration.ofNanos(length)) { parent ->
        chain.channel.map { it.adopt(parent) }.takeWhile { it.timeOffset < length }.toList()
    }
}

internal val defaultAmplitudeGenerator: RandomGenerator.(OrphanNumassEvent?, Long) -> Short = { _, _ -> ((nextDouble() + 2.0) * 100).toShort() }

/**
 * Generate an event chain with fixed count rate
 * @param cr = count rate in Hz
 * @param rnd = random number generator
 * @param amp amplitude generator for the chain. The receiver is rng, first argument is the previous event and second argument
 * is the delay between the next event. The result is the amplitude in channels
 */
fun generateEvents(
        cr: Double,
        rnd: RandomGenerator = defaultGenerator,
        amp: RandomGenerator.(OrphanNumassEvent?, Long) -> Short = defaultAmplitudeGenerator): Chain<OrphanNumassEvent> {
    return MarkovChain(OrphanNumassEvent(rnd.amp(null, 0), 0)) { event ->
        val deltaT = rnd.nextDeltaTime(cr)
        OrphanNumassEvent(rnd.amp(event, deltaT), event.timeOffset + deltaT)
    }
}

/**
 * Generate a chain using provided spectrum for amplitudes
 */
fun generateEvents(
        cr: Double,
        rnd: RandomGenerator = defaultGenerator,
        spectrum: Table): Chain<OrphanNumassEvent> {

    val channels = DoubleArray(spectrum.size())
    val values = DoubleArray(spectrum.size())
    for (i in 0 until spectrum.size()) {
        channels[i] = spectrum.get(CHANNEL_KEY, i).getDouble()
        values[i] = spectrum.get(COUNT_RATE_KEY, i).getDouble()
    }
    val distribution = EnumeratedRealDistribution(channels, values)

    return generateEvents(cr, rnd) { _, _ -> distribution.sample().toShort() }
}

private data class BunchState(var bunchStart: Long = 0, var bunchEnd: Long = 0)

/**
 * The chain of bunched events
 * @param cr count rate of events inside bunch
 * @param bunchRate number of bunches per second
 * @param bunchLength the length of bunch
 */
fun buildBunchChain(
        cr: Double,
        bunchRate: Double,
        bunchLength: Double,
        rnd: RandomGenerator = defaultGenerator,
        amp: RandomGenerator.(OrphanNumassEvent?, Long) -> Short = defaultAmplitudeGenerator
): Chain<OrphanNumassEvent> {
    return StatefulChain(
            BunchState(0, 0),
            OrphanNumassEvent(rnd.amp(null, 0), 0)) { event ->
        if (event.timeOffset >= bunchEnd) {
            bunchStart = bunchEnd + (rnd.nextDeltaTime(bunchRate)).toLong()
            bunchEnd = bunchStart + (bunchLength * 1e9).toLong()
            OrphanNumassEvent(rnd.amp(null, 0), bunchStart)
        } else {
            val deltaT = rnd.nextDeltaTime(cr)
            OrphanNumassEvent(rnd.amp(event, deltaT), event.timeOffset + deltaT)
        }
    }
}

private class MergingState(private val chains: List<Chain<OrphanNumassEvent>>) {
    suspend fun poll(): OrphanNumassEvent {
        val next = chains.minBy { it.value.timeOffset } ?: chains.first()
        val res = next.value
        next.next()
        return res
    }

}

fun mergeEventChains(vararg chains: Chain<OrphanNumassEvent>): Chain<OrphanNumassEvent> {
    return StatefulChain(MergingState(listOf(*chains)), OrphanNumassEvent(0, 0)) {
        poll()
    }
}
//
//
///**
// * @param S - intermediate state of generator
// */
//abstract class ChainGenerator<S> {
//
//    protected abstract fun next(event: NumassEvent?, state: S = buildState()): NumassEvent
//
//    fun buildSequence(): Sequence<NumassEvent> {
//        val state = buildState()
//        return generateSequence(seed = null) { event: NumassEvent? ->
//            next(event, state)
//        }
//    }
//
//    protected abstract fun buildState(): S
//
//    fun generateBlock(start: Instant, length: Long): NumassBlock {
//        val events = buildSequence().takeWhile { it.timeOffset < length }.toList()
//        return SimpleBlock(start, Duration.ofNanos(length), events)
//    }
//}
//
//
//class SimpleChainGenerator(
//        val cr: Double,
//        private val rnd: RandomGenerator = JDKRandomGenerator(),
//        private val amp: RandomGenerator.(NumassEvent?, Long) -> Short = { _, _ -> ((nextDouble() + 2.0) * 100).toShort() }
//) : ChainGenerator<Unit>() {
//
//    override fun buildState() {
//        return Unit
//    }
//
//    override fun next(event: NumassEvent?, state: Unit): NumassEvent {
//        return if (event == null) {
//            NumassEvent(rnd.amp(null, 0), Instant.EPOCH, 0)
//        } else {
//            val deltaT = rnd.nextDeltaTime(cr)
//            NumassEvent(rnd.amp(event, deltaT), event.blockTime, event.timeOffset + deltaT)
//        }
//    }
//
//    fun next(event: NumassEvent?): NumassEvent {
//        return next(event, Unit)
//    }
//}
//
//class BunchGenerator(
//        private val cr: Double,
//        private val bunchRate: Double,
//        private val bunchLength: RandomGenerator.() -> Long,
//        private val rnd: RandomGenerator = JDKRandomGenerator(),
//        private val amp: RandomGenerator.(NumassEvent?, Long) -> Short = { _, _ -> ((nextDouble() + 2.0) * 100).toShort() }
//) : ChainGenerator<BunchGenerator.BunchState>() {
//
//    private val internalGenerator = SimpleChainGenerator(cr, rnd, amp)
//
//    class BunchState(var bunchStart: Long = 0, var bunchEnd: Long = 0)
//
//    override fun next(event: NumassEvent?, state: BunchState): NumassEvent {
//        if (event?.timeOffset ?: 0 >= state.bunchEnd) {
//            state.bunchStart = state.bunchEnd + (rnd.nextExp(bunchRate) * 1e9).toLong()
//            state.bunchEnd = state.bunchStart + rnd.bunchLength()
//            return NumassEvent(rnd.amp(null, 0), Instant.EPOCH, state.bunchStart)
//        } else {
//            return internalGenerator.next(event)
//        }
//    }
//
//    override fun buildState(): BunchState {
//        return BunchState(0, 0)
//    }
//}
//
//
//class MergingGenerator(private vararg val generators: ChainGenerator<*>) : ChainGenerator<MergingGenerator.MergingState>() {
//
//    inner class MergingState {
//        val queue: PriorityQueue<Pair<Sequence<NumassEvent>, NumassEvent>> =
//                PriorityQueue(Comparator.comparing<Pair<Sequence<NumassEvent>, NumassEvent>, Long> { it.second.timeOffset })
//
//        init {
//            generators.forEach { generator ->
//                val sequence = generator.buildSequence()
//                queue.add(Pair(sequence, sequence.iterator().next()))
//            }
//        }
//    }
//
//    override fun next(event: NumassEvent?, state: MergingState): NumassEvent {
//        val pair = state.queue.poll()
//        state.queue.add(Pair(pair.first, pair.first.iterator().next()))
//        return pair.second
//    }
//
//    override fun buildState(): MergingState {
//        return MergingState()
//    }
//}
//
