package inr.numass.data.api

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaHolder

import java.util.stream.Stream

/**
 * A simple static implementation of NumassPoint
 * Created by darksnake on 08.07.2017.
 */
class SimpleNumassPoint : MetaHolder, NumassPoint {
    private val blockList: List<NumassBlock>

    /**
     * Input blocks must be sorted
     * @param voltage
     * @param blocks
     */
    constructor(voltage: Double, blocks: Collection<NumassBlock>) : super(MetaBuilder("point").setValue(NumassPoint.HV_KEY, voltage)) {
        this.blockList = blocks.sortedBy { it.startTime }
    }

    constructor(meta: Meta, blocks: Collection<NumassBlock>) : super(meta) {
        this.blockList = blocks.sortedBy { it.startTime }
    }

    override val blocks: Stream<NumassBlock>
        get() = blockList.stream()
}
