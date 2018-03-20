package inr.numass.data

import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.kodex.nullable
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import inr.numass.data.api.*
import inr.numass.data.storage.ProtoBlock
import java.io.InputStream
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.zip.ZipInputStream
import kotlin.streams.asSequence

/**
 * Created by darksnake on 30-Jan-17.
 */
object NumassDataUtils {
    fun join(name: String, sets: Collection<NumassSet>): NumassSet {
        return object : NumassSet {
            override val points: Stream<out NumassPoint> by lazy {
                val points = sets.stream().flatMap<NumassPoint> { it.points }
                        .collect(Collectors.groupingBy<NumassPoint, Double> { it.voltage })
                points.entries.stream().map { entry -> SimpleNumassPoint(entry.key, entry.value) }
            }

            override val meta: Meta by lazy {
                val metaBuilder = MetaBuilder()
                sets.forEach { set -> metaBuilder.putNode(set.name, set.meta) }
                metaBuilder
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
val Envelope.dataStream: InputStream
    get() = if (this.meta.getString("compression", "none") == "zlib") {
        ZipInputStream(this.data.stream)
    } else {
        this.data.stream
    }

val NumassBlock.channel: Int?
    get() = if (this is ProtoBlock) {
        this.channel
    } else {
        this.meta.optValue("channel").map { it.intValue() }.nullable
    }


fun NumassBlock.transformChain(transform: (NumassEvent, NumassEvent) -> Pair<Short, Long>): NumassBlock {
    return SimpleBlock(this.startTime, this.length, this.meta) { owner ->
        this.events.asSequence()
                .sortedBy { it.timeOffset }
                .zipWithNext(transform).map { NumassEvent(it.first, it.second, owner) }.asIterable()
    }
}

fun NumassBlock.filterChain(condition: (NumassEvent, NumassEvent) -> Boolean): NumassBlock {
    return SimpleBlock(this.startTime, this.length, this.meta) { owner ->
        this.events.asSequence()
                .sortedBy { it.timeOffset }
                .zipWithNext().filter { condition.invoke(it.first, it.second) }.map { it.second }.asIterable()
    }
}

fun NumassBlock.filter(condition: (NumassEvent) -> Boolean): NumassBlock {
    return SimpleBlock(this.startTime, this.length, this.meta) { owner ->
        this.events.asSequence()
                .filter(condition).asIterable()
    }
}

fun NumassBlock.transform(transform: (NumassEvent) -> OrphanNumassEvent): NumassBlock {
    return SimpleBlock(this.startTime, this.length, this.meta) { owner ->
        this.events.asSequence()
                .map { transform(it).adopt(owner) }
                .asIterable()
    }
}