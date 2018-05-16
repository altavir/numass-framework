package inr.numass.data.api

import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

interface ParentBlock : NumassBlock {
    val blocks: List<NumassBlock>
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

    override val events: Stream<NumassEvent>
        get() = blocks.sortedBy { it.startTime }.stream().flatMap { it.events }

    override val frames: Stream<NumassFrame>
        get() = blocks.sortedBy { it.startTime }.stream().flatMap { it.frames }


}
