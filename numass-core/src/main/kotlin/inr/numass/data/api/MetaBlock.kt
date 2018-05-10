package inr.numass.data.api

import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.Stream

interface ParentBlock: NumassBlock{
    val blocks: Collection<NumassBlock>
}

/**
 * A block constructed from a set of other blocks. Internal blocks are not necessary subsequent. Blocks are automatically sorted.
 * Created by darksnake on 16.07.2017.
 */
class MetaBlock(blocks: Collection<NumassBlock>) : ParentBlock {

    override val blocks = TreeSet(Comparator.comparing<NumassBlock, Instant>{ it.startTime })

    init{
        this.blocks.addAll(blocks)
    }

    override val startTime: Instant
        get() = blocks.first().startTime

    override val length: Duration
        get() = Duration.ofNanos(blocks.stream().mapToLong { block -> block.length.toNanos() }.sum())

    override val events: Stream<NumassEvent>
        get() = blocks.stream()
                .sorted(Comparator.comparing<NumassBlock, Instant>{ it.startTime })
                .flatMap{ it.events }

    override val frames: Stream<NumassFrame>
        get() = blocks.stream()
                .sorted(Comparator.comparing<NumassBlock, Instant>{ it.startTime })
                .flatMap{ it.frames }


}
