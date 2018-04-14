package inr.numass.data.api

import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Metoid
import inr.numass.data.storage.ClassicNumassPoint
import inr.numass.data.storage.ProtoNumassPoint
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

/**
 * Created by darksnake on 06-Jul-17.
 */
interface NumassPoint : Metoid, NumassBlock {


    val blocks: Stream<NumassBlock>

    /**
     * Get the voltage setting for the point
     *
     * @return
     */
    val voltage: Double
        get() = meta.getDouble(HV_KEY, 0.0)

    /**
     * Get the index for this point in the set
     * @return
     */
    val index: Int
        get() = meta.getInt(INDEX_KEY, -1)

    /**
     * Get the first block if it exists. Throw runtime exception otherwise.
     *
     * @return
     */
    val firstBlock: NumassBlock
        get() = blocks.findFirst().orElseThrow { RuntimeException("The point is empty") }

    /**
     * Get the starting time from meta or from first block
     *
     * @return
     */
    override val startTime: Instant
        get() = meta.optValue(START_TIME_KEY).map<Instant> { it.timeValue() }.orElseGet { firstBlock.startTime }

    /**
     * Get the length key of meta or calculate length as a sum of block lengths. The latter could be a bit slow
     *
     * @return
     */
    override val length: Duration
        get() = Duration.ofNanos(
                meta.optValue(LENGTH_KEY).map<Long> { it.longValue() }
                        .orElseGet { blocks.mapToLong { it -> it.length.toNanos() }.sum() }
        )

    /**
     * Get all events it all blocks as a single sequence
     *
     *
     * Some performance analysis of different stream concatenation approaches is given here: https://www.techempower.com/blog/2016/10/19/efficient-multiple-stream-concatenation-in-java/
     *
     *
     * @return
     */
    override val events: Stream<NumassEvent>
        get() = blocks.flatMap { it.events }

    /**
     * Get all frames in all blocks as a single sequence
     *
     * @return
     */
    override val frames: Stream<NumassFrame>
        get() = blocks.flatMap { it.frames }

    companion object {

        const val START_TIME_KEY = "start"
        const val LENGTH_KEY = "length"
        const val HV_KEY = "voltage"
        const val INDEX_KEY = "index"

        fun read(envelope: Envelope): NumassPoint {
            return if (envelope.dataType?.startsWith("numass.point.classic") ?: envelope.meta.hasValue("split")) {
                ClassicNumassPoint(envelope)
            } else {
                ProtoNumassPoint(envelope)
            }
        }
    }
}
