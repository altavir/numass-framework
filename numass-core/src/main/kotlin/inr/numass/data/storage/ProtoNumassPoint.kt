package inr.numass.data.storage

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.toList
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import inr.numass.data.NumassProto
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.NumassFrame
import inr.numass.data.api.NumassPoint
import inr.numass.data.dataStream
import inr.numass.data.legacy.NumassFileEnvelope
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.IntStream
import java.util.stream.Stream

/**
 * Protobuf based numass point
 * Created by darksnake on 09.07.2017.
 */
class ProtoNumassPoint(val proto: NumassProto.Point, override val meta: Meta) : NumassPoint {

    override val blocks: Stream<NumassBlock>
        get() = proto.channelsList.stream()
                .flatMap { channel ->
                    channel.blocksList.stream()
                            .map { block -> ProtoBlock(channel.id.toInt(), block, this) }
                            .sorted(Comparator.comparing<ProtoBlock, Instant> { it.startTime })
                }

    override val voltage: Double = meta.getDouble("external_meta.HV1_value", super.voltage)

    override val index: Int = meta.getInt("external_meta.point_index", super.index)

    override val startTime: Instant
        get() = if (meta.hasValue("start_time")) {
            meta.getValue("start_time").getTime()
        } else {
            super.startTime
        }

    companion object {
        fun readFile(path: Path): ProtoNumassPoint {
            return fromEnvelope(NumassFileEnvelope.open(path, true))
        }

        fun fromEnvelope(envelope: Envelope): ProtoNumassPoint {
            val proto = envelope.dataStream.use {
                NumassProto.Point.parseFrom(it)
            }
            return ProtoNumassPoint(proto, envelope.meta)
        }

        fun readFile(path: String, context: Context = Global): ProtoNumassPoint {
            return readFile(context.io.getFile(path).absolutePath)
        }

        fun ofEpochNanos(nanos: Long): Instant {
            val seconds = Math.floorDiv(nanos, 1e9.toInt().toLong())
            val reminder = (nanos % 1e9).toInt()
            return Instant.ofEpochSecond(seconds, reminder.toLong())
        }
    }
}

class ProtoBlock(val channel: Int, private val block: NumassProto.Point.Channel.Block, parent: NumassBlock? = null) : NumassBlock {
    override val meta: Meta by lazy {
        val blockMeta = buildMeta {
            "channel" to channel
        }
        return@lazy parent?.let { Laminate(blockMeta, parent.meta) } ?: blockMeta
    }

    override val startTime: Instant
        get() = ProtoNumassPoint.ofEpochNanos(block.time)

    override val length: Duration = when {
        block.length > 0 -> Duration.ofNanos(block.length)
        meta.hasValue("acquisition_time") -> Duration.ofMillis((meta.getDouble("acquisition_time") * 1000).toLong())
        else -> {
            LoggerFactory.getLogger(javaClass).error("No length information on block. Trying to infer from first and last events")
            val times = events.map { it.timeOffset }.toList()
            val nanos = (times.max()!! - times.min()!!)
            Duration.ofNanos(nanos)
        }
    }

    override val events: Stream<NumassEvent>
        get() = if (block.hasEvents()) {
            val events = block.events
            IntStream.range(0, events.timesCount).mapToObj { i -> NumassEvent(events.getAmplitudes(i).toShort(), events.getTimes(i), this) }
        } else {
            Stream.empty()
        }


    override val frames: Stream<NumassFrame>
        get() {
            val tickSize = Duration.ofNanos(block.binSize)
            return block.framesList.stream().map { frame ->
                val time = startTime.plusNanos(frame.time)
                val data = frame.data.asReadOnlyByteBuffer()
                NumassFrame(time, tickSize, data.asShortBuffer())
            }
        }
}