package inr.numass.data.api

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaHolder

/**
 * A simple static implementation of NumassPoint
 * Created by darksnake on 08.07.2017.
 */
class SimpleNumassPoint(override val blocks: List<NumassBlock>, meta: Meta) : MetaHolder(meta), NumassPoint {

    /**
     * Input blocks must be sorted
     * @param voltage
     * @param blocks
     */
    constructor(blocks: Collection<NumassBlock>, voltage: Double) :
            this(blocks.sortedBy { it.startTime }, MetaBuilder("point").setValue(NumassPoint.HV_KEY, voltage))

    init {
        if(blocks.isEmpty()){
            throw IllegalArgumentException("No blocks in collection")
        }
    }

}
