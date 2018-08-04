package inr.numass.data

import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import inr.numass.data.api.*
import inr.numass.data.storage.ProtoBlock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.stream.Collectors
import java.util.zip.Inflater
import kotlin.streams.asSequence
import kotlin.streams.toList


/**
 * Created by darksnake on 30-Jan-17.
 */
object NumassDataUtils {
    fun join(setName: String, sets: Collection<NumassSet>): NumassSet {
        return object : NumassSet {
            override suspend fun getHvData() = TODO()

            override val points: List<NumassPoint> by lazy {
                val points = sets.stream().flatMap<NumassPoint> { it.points.stream() }
                        .collect(Collectors.groupingBy<NumassPoint, Double> { it.voltage })
                points.entries.stream().map { entry -> SimpleNumassPoint(entry.value, entry.key) }
                        .toList()
            }

            override val meta: Meta by lazy {
                val metaBuilder = MetaBuilder()
                sets.forEach { set -> metaBuilder.putNode(set.name, set.meta) }
                metaBuilder
            }

            override val name = setName
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
        //TODO move to new type of data
        val inflatter = Inflater()
        val array: ByteArray = with(data.buffer) {
            if (hasArray()) {
                array()
            } else {
                ByteArray(this.limit()).also {
                    this.position(0)
                    get(it)
                }
            }
        }
        inflatter.setInput(array)
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (!inflatter.finished()) {
            val size = inflatter.inflate(buffer)
            bos.write(buffer, 0, size)
        }
        val unzippeddata = bos.toByteArray()
        inflatter.end()
        ByteArrayInputStream(unzippeddata)
    } else {
        this.data.stream
    }

val NumassBlock.channel: Int
    get() = if (this is ProtoBlock) {
        this.channel
    } else {
        0
    }


suspend fun NumassBlock.transformChain(transform: (NumassEvent, NumassEvent) -> Pair<Short, Long>?): NumassBlock {
    return SimpleBlock.produce(this.startTime, this.length) {
        this.events.asSequence()
                .sortedBy { it.timeOffset }
                .zipWithNext(transform)
                .filterNotNull()
                .map { OrphanNumassEvent(it.first, it.second) }.asIterable()
    }
}

suspend fun NumassBlock.filterChain(condition: (NumassEvent, NumassEvent) -> Boolean): NumassBlock {
    return SimpleBlock.produce(this.startTime, this.length) {
        this.events.asSequence()
                .sortedBy { it.timeOffset }
                .zipWithNext().filter { condition.invoke(it.first, it.second) }.map { it.second }.asIterable()
    }
}

suspend fun NumassBlock.filter(condition: (NumassEvent) -> Boolean): NumassBlock {
    return SimpleBlock.produce(this.startTime, this.length) {
        this.events.asSequence().filter(condition).asIterable()
    }
}

suspend fun NumassBlock.transform(transform: (NumassEvent) -> OrphanNumassEvent): NumassBlock {
    return SimpleBlock.produce(this.startTime, this.length) {
        this.events.asSequence()
                .map { transform(it) }
                .asIterable()
    }
}