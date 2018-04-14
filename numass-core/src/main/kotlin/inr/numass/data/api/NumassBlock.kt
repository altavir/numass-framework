package inr.numass.data.api

import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import inr.numass.data.channel
import kotlinx.coroutines.experimental.runBlocking
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream


/**
 * A single numass event with given amplitude and time.
 *
 * @author Darksnake
 * @property amp the amplitude of the event
 * @property timeOffset time in nanoseconds relative to block start
 * @property owner an owner block for this event
 *
 */
class NumassEvent(val amp: Short, val timeOffset: Long, val owner: NumassBlock) : Serializable {

    val channel: Int?
        get() = owner.channel

    val time: Instant
        get() = owner.startTime.plusNanos(timeOffset)

}


/**
 * A single continuous measurement block. The block can contain both isolated events and signal frames
 *
 *
 * Created by darksnake on 06-Jul-17.
 */
interface NumassBlock : Metoid {

    /**
     * The absolute start time of the block
     */
    val startTime: Instant

    /**
     * The length of the block
     */
    val length: Duration

    /**
     * Stream of isolated events. Could be empty
     */
    val events: Stream<NumassEvent>

    /**
     * Stream of frames. Could be empty
     */
    val frames: Stream<NumassFrame>
}


typealias OrphanNumassEvent = Pair<Short, Long>

fun OrphanNumassEvent.adopt(parent: NumassBlock): NumassEvent {
    return NumassEvent(this.first, this.second, parent)
}

val OrphanNumassEvent.timeOffset: Long
    get() = this.second

val OrphanNumassEvent.amp: Short
    get() = this.first


/**
 * A simple in-memory implementation of block of events. No frames are allowed
 * Created by darksnake on 08.07.2017.
 */
class SimpleBlock(
        override val startTime: Instant,
        override val length: Duration,
        override val meta: Meta = Meta.empty(),
        producer: suspend (NumassBlock) -> Iterable<NumassEvent>) : NumassBlock, Serializable {

    private val eventList = runBlocking { producer(this@SimpleBlock).toList()}

    override val frames: Stream<NumassFrame> = Stream.empty()

    override val events: Stream<NumassEvent>
        get() = eventList.stream()

}