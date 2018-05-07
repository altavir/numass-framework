package inr.numass.data.storage

import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.NumassFrame
import inr.numass.data.api.NumassPoint
import inr.numass.data.legacy.NumassFileEnvelope
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * Created by darksnake on 08.07.2017.
 */
class ClassicNumassPoint(private val envelope: Envelope) : NumassPoint {

    override val meta: Meta = envelope.meta

    override val voltage: Double = meta.getDouble("external_meta.HV1_value", super.voltage)

    override val index: Int = meta.getInt("external_meta.point_index", super.index)

    override val blocks: List<NumassBlock> by lazy {
        val length: Long = if (envelope.meta.hasValue("external_meta.acquisition_time")) {
            envelope.meta.getValue("external_meta.acquisition_time").long
        } else {
            envelope.meta.getValue("acquisition_time").long
        }
        listOf(ClassicBlock(startTime, Duration.ofSeconds(length)))
    }

    override val startTime: Instant
        get() = if (meta.hasValue("start_time")) {
            meta.getValue("start_time").time
        } else {
            super.startTime
        }


    //TODO split blocks using meta
    private inner class ClassicBlock(
            override val startTime: Instant,
            override val length: Duration) : NumassBlock, Iterable<NumassEvent> {

        override val events: Stream<NumassEvent>
            get() = StreamSupport.stream(this.spliterator(), false)

        override fun iterator(): Iterator<NumassEvent> {
            val timeCoef = envelope.meta.getDouble("time_coeff", 50.0)
            try {
                val buffer = ByteBuffer.allocate(7000)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                val channel = envelope.data.channel
                channel.read(buffer)
                buffer.flip()
                return object : Iterator<NumassEvent> {

                    override fun hasNext(): Boolean {
                        try {
                            return if (buffer.hasRemaining()) {
                                true
                            } else {
                                buffer.flip()
                                val num = channel.read(buffer)
                                if (num > 0) {
                                    buffer.flip()
                                    true
                                } else {
                                    false
                                }
                            }
                        } catch (e: IOException) {
                            LoggerFactory.getLogger(this@ClassicNumassPoint.javaClass).error("Unexpected IOException when reading block", e)
                            return false
                        }

                    }

                    override fun next(): NumassEvent {
                        val amp = java.lang.Short.toUnsignedInt(buffer.short).toShort()
                        val time = Integer.toUnsignedLong(buffer.int)
                        val status = buffer.get() // status is ignored
                        return NumassEvent(amp, (time * timeCoef).toLong(), this@ClassicBlock)
                    }
                }
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }

        }


        override val frames: Stream<NumassFrame>
            get() = Stream.empty()
    }

    companion object {
        fun readFile(path: Path): ClassicNumassPoint {
            return ClassicNumassPoint(NumassFileEnvelope.open(path, true))
        }
    }
}
