package inr.numass.data.api

import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.Stream

/**
 * A block constructed from a set of other blocks. Internal blocks are not necessary subsequent. Blocks are automatically sorted.
 * Created by darksnake on 16.07.2017.
 */
class MetaBlock : NumassBlock {
    private val blocks = TreeSet(Comparator.comparing<NumassBlock, Instant>{ it.startTime })

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

    constructor(vararg blocks: NumassBlock) {
        this.blocks.addAll(Arrays.asList(*blocks))
    }

    constructor(blocks: Collection<NumassBlock>) {
        this.blocks.addAll(blocks)
    }
}
