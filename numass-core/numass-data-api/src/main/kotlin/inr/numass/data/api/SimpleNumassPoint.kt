package inr.numass.data.api

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaHolder
import hep.dataforge.meta.buildMeta

/**
 * A simple static implementation of NumassPoint
 * Created by darksnake on 08.07.2017.
 */
class SimpleNumassPoint(override val blocks: List<NumassBlock>, meta: Meta, override val isSequential: Boolean = true) :
    MetaHolder(meta), NumassPoint {

    init {
        if (blocks.isEmpty()) {
            throw IllegalArgumentException("No blocks in collection")
        }
    }

    companion object {
        fun build(blocks: Collection<NumassBlock>, voltage: Double? = null, index: Int? = null): SimpleNumassPoint {
            val meta = buildMeta("point") {
                NumassPoint.HV_KEY to voltage
                NumassPoint.INDEX_KEY to index
            }
            return SimpleNumassPoint(blocks.sortedBy { it.startTime }, meta.build())
        }
    }

}
