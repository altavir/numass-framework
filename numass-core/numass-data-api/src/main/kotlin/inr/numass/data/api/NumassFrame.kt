package inr.numass.data.api

import java.nio.ShortBuffer
import java.time.Duration
import java.time.Instant

/**
 * The continuous frame of digital detector data
 * Created by darksnake on 06-Jul-17.
 */
class NumassFrame(
    /**
     * The absolute start time of the frame
     */
    val time: Instant,
    /**
     * The time interval per tick
     */
    val tickSize: Duration,
    /**
     * The buffered signal shape in ticks
     */
    val signal: ShortBuffer
) {

    val length: Duration
        get() = tickSize.multipliedBy(signal.capacity().toLong())
}
