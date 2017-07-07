package inr.numass.viewer

import hep.dataforge.data.Data
import hep.dataforge.meta.Meta
import hep.dataforge.tables.Table
import java.time.Instant
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Cached numass data
 * Created by darksnake on 23-Jun-17.
 */
class NumassDataCache(val data: NumassData) {
    private val cachedDescription: String by lazy { data.description }
    private val cachedMeta: Meta by lazy { data.meta }
    private val cachedPoints: List<NumassPoint> by lazy { data.stream().collect(Collectors.toList()) }
    private val hv: Table by lazy { data.hvData.get() }

    override fun getDescription(): String {
        return cachedDescription
    }

    override fun meta(): Meta {
        return cachedMeta
    }

    override fun stream(): Stream<NumassPoint> {
        return cachedPoints.stream();
    }

    override fun isEmpty(): Boolean {
        return data.isEmpty
    }

    override fun startTime(): Instant {
        return data.startTime()
    }

    override fun getName(): String {
        return data.name;
    }

    override fun getHVData(): Data<Table> {
        return Data.buildStatic(hv);
    }
}