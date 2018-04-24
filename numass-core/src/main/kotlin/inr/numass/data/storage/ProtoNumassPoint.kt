package inr.numass.data.storage

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.kodex.toList
import hep.dataforge.meta.Meta
import inr.numass.data.NumassProto
import inr.numass.data.api.*
import inr.numass.data.dataStream
import inr.numass.data.legacy.NumassFileEnvelope
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.stream.IntStream
import java.util.stream.Stream

/**
 * Protobuf based numass point
 * Created by darksnake on 09.07.2017.
 */
class ProtoNumassPoint(override val meta: Meta, val protoBuilder: () -> NumassProto.Point) : NumassPoint {

    val proto: NumassProto.Point
        get() = protoBuilder()

    override val blocks: List<NumassBlock>
        get() = proto.channelsList
                .flatMap { channel ->
                    channel.blocksList
                            .map { block -> ProtoBlock(channel.id.toInt(), block, this) }
                            .sortedBy { it.startTime }
                }

    override val channels: Map<Int, NumassBlock>
        get() = proto.channelsList.groupBy { it.id.toInt() }.mapValues { entry ->
            MetaBlock(entry.value.flatMap { it.blocksList }.map { ProtoBlock(entry.key, it, this) })
        }

    override val voltage: Double = meta.getDouble("external_meta.HV1_value", super.voltage)

    override val index: Int = meta.getInt("external_meta.point_index", super.index)

    override val startTime: Instant
        get() = if (meta.hasValue("start_time")) {
            meta.getValue("start_time").time
        } else {
            super.startTime
        }

    override val length: Duration
        get() = if (meta.hasValue("acquisition_time")) {
            Duration.ofMillis((meta.getDouble("acquisition_time") * 1000).toLong())
        } else {
            super.length
        }


    companion object {
        fun readFile(path: Path): ProtoNumassPoint {
            return fromEnvelope(NumassFileEnvelope.open(path, true))
        }

        fun fromEnvelope(envelope: Envelope): ProtoNumassPoint {
            return ProtoNumassPoint(envelope.meta) {
                envelope.dataStream.use {
                    NumassProto.Point.parseFrom(it)
                }
            }
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

class ProtoBlock(val channel: Int, private val block: NumassProto.Point.Channel.Block, val parent: NumassPoint? = null) : NumassBlock {

    override val startTime: Instant
        get() = ProtoNumassPoint.ofEpochNanos(block.time)

    override val length: Duration = when {
        block.length > 0 -> Duration.ofNanos(block.length)
        parent?.meta?.hasValue("acquisition_time") ?: false ->
            Duration.ofMillis((parent!!.meta.getDouble("acquisition_time") * 1000).toLong())
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