package inr.numass.data.storage

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import inr.numass.data.NumassProto
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.NumassFrame
import inr.numass.data.api.NumassPoint
import inr.numass.data.dataStream
import inr.numass.data.legacy.NumassFileEnvelope
import java.io.IOException
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
class ProtoNumassPoint(private val envelope: Envelope) : NumassPoint {

    private val point: NumassProto.Point
        get() = try {
            envelope.dataStream.use { stream -> return NumassProto.Point.parseFrom(stream) }
        } catch (ex: IOException) {
            throw RuntimeException("Failed to read point via protobuf")
        }

    override val blocks: Stream<NumassBlock>
        get() = point.channelsList.stream()
                .flatMap { channel ->
                    channel.blocksList.stream()
                            .map { block -> ProtoBlock(channel.num.toInt(), block, meta) }
                            .sorted(Comparator.comparing<ProtoBlock, Instant> { it.startTime })
                }


    override fun getMeta(): Meta {
        return envelope.meta
    }

    companion object {
        fun readFile(path: Path): ProtoNumassPoint {
            return ProtoNumassPoint(NumassFileEnvelope.open(path, true))
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

class ProtoBlock(override val channel: Int, private val block: NumassProto.Point.Channel.Block, private val meta: Meta) : NumassBlock {

    override val startTime: Instant
        get() = ProtoNumassPoint.ofEpochNanos(block.time)

    override val length: Duration
        get() = Duration.ofNanos((meta.getDouble("params.b_size") / meta.getDouble("params.sample_freq") * 1e9).toLong())


    override val events: Stream<NumassEvent>
        get() = if (block.hasEvents()) {
            val events = block.events
            IntStream.range(0, events.timesCount).mapToObj { i -> NumassEvent(events.getAmplitudes(i).toShort(), events.getTimes(i), this) }
        } else {
            Stream.empty()
        }


    override val frames: Stream<NumassFrame>
        get() {
            val tickSize = Duration.ofNanos((1e9 / meta.getInt("params.sample_freq")).toLong())
            return block.framesList.stream().map { frame ->
                val time = startTime.plusNanos(frame.time)
                val data = frame.data.asReadOnlyByteBuffer()
                NumassFrame(time, tickSize, data.asShortBuffer())
            }
        }
}