package inr.numass.data.api

import inr.numass.data.channel
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

open class OrphanNumassEvent(val amplitude: Short, val timeOffset: Long) : Serializable, Comparable<OrphanNumassEvent> {
    operator fun component1() = amplitude
    operator fun component2() = timeOffset

    override fun compareTo(other: OrphanNumassEvent): Int {
        return this.timeOffset.compareTo(other.timeOffset)
    }
}

/**
 * A single numass event with given amplitude and time.
 *
 * @author Darksnake
 * @property amp the amplitude of the event
 * @property timeOffset time in nanoseconds relative to block start
 * @property owner an owner block for this event
 *
 */
class NumassEvent(amplitude: Short, timeOffset: Long, val owner: NumassBlock) : OrphanNumassEvent(amplitude, timeOffset), Serializable {

    val channel: Int
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
interface NumassBlock {

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

fun OrphanNumassEvent.adopt(parent: NumassBlock): NumassEvent {
    return NumassEvent(this.amplitude, this.timeOffset, parent)
}

/**
 * A simple in-memory implementation of block of events. No frames are allowed
 * Created by darksnake on 08.07.2017.
 */
class SimpleBlock(
        override val startTime: Instant,
        override val length: Duration,
        rawEvents: Iterable<OrphanNumassEvent>
) : NumassBlock, Serializable {

    private val eventList by lazy { rawEvents.map { it.adopt(this) } }

    override val frames: Stream<NumassFrame> get() = Stream.empty()

    override val events: Stream<NumassEvent>
        get() = eventList.stream()

    companion object {
        suspend fun produce(startTime: Instant, length: Duration, producer: suspend () -> Iterable<OrphanNumassEvent>): SimpleBlock {
            return SimpleBlock(startTime, length, producer())
        }
    }
}