package inr.numass.data.api

import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

/**
 * A simple in-memory implementation of block of events. No frames are allowed
 * Created by darksnake on 08.07.2017.
 */
class SimpleBlock(override val startTime: Instant, override val length: Duration, private val eventList: List<NumassEvent>) : NumassBlock, Serializable {

    override val frames: Stream<NumassFrame> = Stream.empty()

    override val events: Stream<NumassEvent>
        get() = eventList.stream()

}
