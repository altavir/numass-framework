package inr.numass.viewer

import hep.dataforge.meta.Meta
import hep.dataforge.tables.Table
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Cached numass data
 * Created by darksnake on 23-Jun-17.
 */
class NumassDataCache(val data: NumassSet) : NumassSet {
    private val cachedDescription: String by lazy { data.description }
    private val cachedMeta: Meta by lazy { data.meta }
    private val cachedPoints: List<NumassPoint> by lazy { data.points.collect(Collectors.toList()) }
    private val hv: Optional<Table> by lazy { data.hvData }


    override fun getPoints(): Stream<NumassPoint> {
        return cachedPoints.stream();
    }

    override fun getDescription(): String {
        return cachedDescription
    }

    override fun getMeta(): Meta {
        return cachedMeta
    }

    override fun getName(): String {
        return data.name;
    }

    override fun getHvData(): Optional<Table> {
        return hv;
    }
}