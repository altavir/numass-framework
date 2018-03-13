package inr.numass.data

import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SimpleNumassPoint
import java.io.InputStream
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.zip.ZipInputStream

/**
 * Created by darksnake on 30-Jan-17.
 */
object NumassDataUtils {
    fun join(name: String, sets: Collection<NumassSet>): NumassSet {
        return object : NumassSet {
            override fun getPoints(): Stream<NumassPoint> {
                val points = sets.stream().flatMap<NumassPoint> { it.points }
                        .collect(Collectors.groupingBy<NumassPoint, Double> { it.voltage })
                return points.entries.stream().map { entry -> SimpleNumassPoint(entry.key, entry.value) }
            }

            override fun getMeta(): Meta {
                val metaBuilder = MetaBuilder()
                sets.forEach { set -> metaBuilder.putNode(set.name, set.meta) }
                return metaBuilder
            }

            override fun getName(): String {
                return name
            }
        }
    }

    fun adapter(): SpectrumAdapter {
        return SpectrumAdapter("Uset", "CR", "CRerr", "Time")
    }
}

/**
 * Get valid data stream utilizing compression if it is present
 */
val Envelope.dataStream : InputStream
    get() = if(this.meta.getString("compression", "none") == "zlib"){
        ZipInputStream(this.data.stream)
    } else {
        this.data.stream
    }
