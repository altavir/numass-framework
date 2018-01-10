package inr.numass.data

import hep.dataforge.maths.chain.Chain
import hep.dataforge.maths.chain.MarkovChain
import hep.dataforge.maths.chain.StatefulChain
import hep.dataforge.stat.defaultGenerator
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.SimpleBlock
import kotlinx.coroutines.experimental.channels.takeWhile
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import org.apache.commons.math3.random.RandomGenerator
import java.time.Duration
import java.time.Instant

private fun RandomGenerator.nextExp(mean: Double): Double {
    return -mean * Math.log(1 - nextDouble())
}

private fun RandomGenerator.nextDeltaTime(cr: Double): Long {
    return (nextExp(1.0 / cr) * 1e9).toLong()
}

fun generateBlock(start: Instant, length: Long, chain: Chain<NumassEvent>): NumassBlock {

    val events = runBlocking { chain.channel.takeWhile { it.timeOffset < length }.toList()}
    return SimpleBlock(start, Duration.ofNanos(length), events)
}

internal val defaultAmplitudeGenerator: RandomGenerator.(NumassEvent?, Long) -> Short = { _, _ -> ((nextDouble() + 2.0) * 100).toShort() }

fun buildSimpleEventChain(
        cr: Double,
        rnd: RandomGenerator = defaultGenerator,
        amp: RandomGenerator.(NumassEvent?, Long) -> Short = defaultAmplitudeGenerator): Chain<NumassEvent> {
    return MarkovChain(NumassEvent(rnd.amp(null, 0), Instant.now(), 0)) { event ->
        val deltaT = rnd.nextDeltaTime(cr)
        NumassEvent(rnd.amp(event, deltaT), event.blockTime, event.timeOffset + deltaT)
    }
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
        amp: RandomGenerator.(NumassEvent?, Long) -> Short = defaultAmplitudeGenerator
): Chain<NumassEvent> {
    return StatefulChain(
            BunchState(0, 0),
            NumassEvent(rnd.amp(null, 0), Instant.now(), 0)) { event ->
        if (event.timeOffset >= bunchEnd) {
            bunchStart = bunchEnd + (rnd.nextDeltaTime(bunchRate)).toLong()
            bunchEnd = bunchStart + (bunchLength*1e9).toLong()
            NumassEvent(rnd.amp(null, 0), Instant.EPOCH, bunchStart)
        } else {
            val deltaT = rnd.nextDeltaTime(cr)
            NumassEvent(rnd.amp(event, deltaT), event.blockTime, event.timeOffset + deltaT)
        }
    }
}

private class MergingState(private val chains: List<Chain<NumassEvent>>) {
    suspend fun poll(): NumassEvent {
        val next = chains.minBy { it.value.timeOffset } ?: chains.first()
        val res = next.value
        next.next()
        return res
    }

}

fun mergeEventChains(vararg chains: Chain<NumassEvent>): Chain<NumassEvent> {
    return StatefulChain(MergingState(listOf(*chains)),NumassEvent(0, Instant.now(), 0)){
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
