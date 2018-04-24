package inr.numass.data

import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.tables.Table
import inr.numass.data.api.*
import inr.numass.data.storage.ProtoBlock
import kotlinx.coroutines.experimental.Deferred
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
            override val hvData: Deferred<Table?>
                get() = TODO("Join hv tables")

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
        inflatter.setInput(data.buffer.array())
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


fun NumassBlock.transformChain(transform: (NumassEvent, NumassEvent) -> Pair<Short, Long>?): NumassBlock {
    return SimpleBlock(this.startTime, this.length) { owner ->
        this.events.asSequence()
                .sortedBy { it.timeOffset }
                .zipWithNext(transform)
                .filterNotNull()
                .map { NumassEvent(it.first, it.second, owner) }.asIterable()
    }
}

fun NumassBlock.filterChain(condition: (NumassEvent, NumassEvent) -> Boolean): NumassBlock {
    return SimpleBlock(this.startTime, this.length) { owner ->
        this.events.asSequence()
                .sortedBy { it.timeOffset }
                .zipWithNext().filter { condition.invoke(it.first, it.second) }.map { it.second }.asIterable()
    }
}

fun NumassBlock.filter(condition: (NumassEvent) -> Boolean): NumassBlock {
    return SimpleBlock(this.startTime, this.length) { owner ->
        this.events.asSequence().filter(condition).asIterable()
    }
}

fun NumassBlock.transform(transform: (NumassEvent) -> OrphanNumassEvent): NumassBlock {
    return SimpleBlock(this.startTime, this.length) { owner ->
        this.events.asSequence()
                .map { transform(it).adopt(owner) }
                .asIterable()
    }
}