package inr.numass.data.api

import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

interface ParentBlock : NumassBlock {
    val blocks: List<NumassBlock>

    /**
     * If true, the sub-blocks a considered to be isSequential, if not, the sub-blocks are parallel
     */
    val isSequential: Boolean
        get() = true
}

/**
 * A block constructed from a set of other blocks. Internal blocks are not necessary subsequent. Blocks are automatically sorted.
 * Created by darksnake on 16.07.2017.
 */
class MetaBlock(override val blocks: List<NumassBlock>) : ParentBlock {

    override val startTime: Instant
        get() = blocks.first().startTime

    override val length: Duration
        get() = Duration.ofNanos(blocks.stream().mapToLong { block -> block.length.toNanos() }.sum())

    /**
     * A stream of events, sorted by block time but not sorted by event time
     */
    override val events: Stream<NumassEvent>
        get() = blocks.sortedBy { it.startTime }.stream().flatMap { it.events }

    override val frames: Stream<NumassFrame>
        get() = blocks.sortedBy { it.startTime }.stream().flatMap { it.frames }


}
