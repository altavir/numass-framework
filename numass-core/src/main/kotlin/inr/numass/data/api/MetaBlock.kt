package inr.numass.data.api

import hep.dataforge.meta.Meta
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.Stream

/**
 * A block constructed from a set of other blocks. Internal blocks are not necessary subsequent. Blocks are automatically sorted.
 * Created by darksnake on 16.07.2017.
 */
class MetaBlock(blocks: Collection<NumassBlock>, override val meta: Meta = Meta.empty()) : NumassBlock {

    private val blocks = TreeSet(Comparator.comparing<NumassBlock, Instant>{ it.startTime })

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
