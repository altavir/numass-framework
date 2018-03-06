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

    override fun getBlocks(): Stream<NumassBlock> {
        //        double u = envelope.meta().getDouble("external_meta.HV1_value", 0);
        val length: Long
        if (envelope.meta.hasValue("external_meta.acquisition_time")) {
            length = envelope.meta.getValue("external_meta.acquisition_time").longValue()
        } else {
            length = envelope.meta.getValue("acquisition_time").longValue()
        }
        return Stream.of(ClassicBlock(startTime, Duration.ofSeconds(length)))
    }

    override fun getStartTime(): Instant {
        return if (meta.hasValue("start_time")) {
            meta.getValue("start_time").timeValue()
        } else {
            Instant.EPOCH
        }
    }

    override fun getVoltage(): Double {
        return meta.getDouble("external_meta.HV1_value", 0.0)
    }

    override fun getIndex(): Int {
        return meta.getInt("external_meta.point_index", -1)
    }

    override fun getMeta(): Meta {
        return envelope.meta
    }

    //TODO split blocks using meta
    private inner class ClassicBlock
    //        private final long blockOffset;

    (private val startTime: Instant, private val length: Duration)//            this.blockOffset = blockOffset;
        : NumassBlock, Iterable<NumassEvent> {

        override fun getStartTime(): Instant {
            return startTime
        }

        override fun getLength(): Duration {
            return length
        }

        override fun getEvents(): Stream<NumassEvent> {
            return StreamSupport.stream(this.spliterator(), false)
        }

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
                            if (buffer.hasRemaining()) {
                                return true
                            } else {
                                buffer.flip()
                                val num = channel.read(buffer)
                                if (num > 0) {
                                    buffer.flip()
                                    return true
                                } else {
                                    return false
                                }
                            }
                        } catch (e: IOException) {
                            LoggerFactory.getLogger(this@ClassicNumassPoint.javaClass).error("Unexpected IOException when reading block", e)
                            return false
                        }

                    }

                    override fun next(): NumassEvent {
                        val channel = java.lang.Short.toUnsignedInt(buffer.short).toShort()
                        val time = Integer.toUnsignedLong(buffer.int)
                        val status = buffer.get() // status is ignored
                        return NumassEvent(channel, startTime, (time * timeCoef).toLong())
                    }
                }
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }

        }


        override fun getFrames(): Stream<NumassFrame> {
            return Stream.empty()
        }
    }

    companion object {
        fun readFile(path: Path): ClassicNumassPoint {
            return ClassicNumassPoint(NumassFileEnvelope.open(path, true))
        }
    }
}
