package inr.numass.data.storage

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

    override fun getBlocks(): Stream<NumassBlock> {
        return point.channelsList.stream()
                .flatMap { channel ->
                    channel.blocksList.stream()
                            .map { block -> ProtoBlock(channel.num.toInt(), block, meta) }
                            .sorted(Comparator.comparing<ProtoBlock, Instant> { it.startTime })
                }
    }

    override fun getMeta(): Meta {
        return envelope.meta
    }

    companion object {
        fun readFile(path: Path): ProtoNumassPoint {
            return ProtoNumassPoint(NumassFileEnvelope.open(path, true))
        }

        fun ofEpochNanos(nanos: Long): Instant {
            val seconds = Math.floorDiv(nanos, 1e9.toInt().toLong())
            val reminder = (nanos % 1e9).toInt()
            return Instant.ofEpochSecond(seconds, reminder.toLong())
        }
    }
}

class ProtoBlock(val channel: Int, private val block: NumassProto.Point.Channel.Block, private val meta: Meta) : NumassBlock {

    override fun getStartTime(): Instant {
        return ProtoNumassPoint.ofEpochNanos(block.time)
    }

    override fun getLength(): Duration {
        return Duration.ofNanos((meta.getDouble("params.b_size") / meta.getDouble("params.sample_freq") * 1e9).toLong())
    }

    override fun getEvents(): Stream<NumassEvent> {
        val blockTime = startTime
        if (block.hasEvents()) {
            val events = block.events
            return IntStream.range(0, events.timesCount).mapToObj { i -> NumassEvent(events.getAmplitudes(i).toShort(), blockTime, events.getTimes(i)) }
        } else {
            return Stream.empty()
        }
    }

    override fun getFrames(): Stream<NumassFrame> {
        val tickSize = Duration.ofNanos((1e9 / meta.getInt("params.sample_freq")).toLong())
        return block.framesList.stream().map { frame ->
            val time = startTime.plusNanos(frame.time)
            val data = frame.data.asReadOnlyByteBuffer()
            NumassFrame(time, tickSize, data.asShortBuffer())
        }
    }
}