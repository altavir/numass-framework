/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package inr.numass.data.api

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

    override fun toString(): String {
        return "[$amplitude, $timeOffset]"
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

    val channel: Int get() = 0
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