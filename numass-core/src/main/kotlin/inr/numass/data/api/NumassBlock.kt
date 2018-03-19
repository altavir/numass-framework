package inr.numass.data.api

import java.time.Duration
import java.time.Instant
import java.util.stream.Stream


typealias NumassChannel = Int



/**
 * A single continuous measurement block. The block can contain both isolated events and signal frames
 *
 *
 * Created by darksnake on 06-Jul-17.
 */
interface NumassBlock {

    /**
     * A channel
     */
    val channel: NumassChannel
        get() = DEFAULT_CHANNEL

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

    companion object {
        val DEFAULT_CHANNEL: NumassChannel = -1
    }
}
