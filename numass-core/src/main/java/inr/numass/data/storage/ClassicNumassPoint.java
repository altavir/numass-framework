package inr.numass.data.storage;

import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.NumassFrame;
import inr.numass.data.api.NumassPoint;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by darksnake on 08.07.2017.
 */
public class ClassicNumassPoint implements NumassPoint {
    private final Envelope envelope;

    public ClassicNumassPoint(Envelope envelope) {
        this.envelope = envelope;
    }

    @Override
    public Stream<NumassBlock> getBlocks() {
//        double u = envelope.meta().getDouble("external_meta.HV1_value", 0);
        long length;
        if (envelope.getMeta().hasValue("external_meta.acquisition_time")) {
            length = envelope.getMeta().getValue("external_meta.acquisition_time").longValue();
        } else {
            length = envelope.getMeta().getValue("acquisition_time").longValue();
        }
        return Stream.of(new ClassicBlock(getStartTime(), Duration.ofSeconds(length)));
    }

    @Override
    public Instant getStartTime() {
        if (getMeta().hasValue("start_time")) {
            return getMeta().getValue("start_time").timeValue();
        } else {
            return Instant.EPOCH;
        }
    }

    @Override
    public double getVoltage() {
        return getMeta().getDouble("external_meta.HV1_value", 0);
    }

    @Override
    public Meta getMeta() {
        return envelope.getMeta();
    }

    //TODO split blocks using meta
    private class ClassicBlock implements NumassBlock, Iterable<NumassEvent> {
        private final Instant startTime;
        private final Duration length;
//        private final long blockOffset;

        public ClassicBlock(Instant startTime, Duration length) {
            this.startTime = startTime;
            this.length = length;
//            this.blockOffset = blockOffset;
        }

        @Override
        public Instant getStartTime() {
            return startTime;
        }

        @Override
        public Duration getLength() {
            return length;
        }

        @Override
        public Stream<NumassEvent> getEvents() {
            return StreamSupport.stream(this.spliterator(), false);
        }

        @NotNull
        @Override
        public Iterator<NumassEvent> iterator() {
            double timeCoef = envelope.getMeta().getDouble("time_coeff", 50);
            try {
                ByteBuffer buffer = ByteBuffer.allocate(7000);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                ReadableByteChannel channel = envelope.getData().getChannel();
                channel.read(buffer);
                buffer.flip();
                return new Iterator<NumassEvent>() {

                    @Override
                    public boolean hasNext() {
                        try {
                            if (buffer.hasRemaining()) {
                                return true;
                            } else {
                                buffer.flip();
                                int num = channel.read(buffer);
                                if (num > 0) {
                                    buffer.flip();
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        } catch (IOException e) {
                            LoggerFactory.getLogger(ClassicNumassPoint.this.getClass()).error("Unexpected IOException when reading block", e);
                            return false;
                        }
                    }

                    @Override
                    public NumassEvent next() {
                        short channel = (short) Short.toUnsignedInt(buffer.getShort());
                        long time = Integer.toUnsignedLong(buffer.getInt());
                        byte status = buffer.get(); // status is ignored
                        return new NumassEvent(channel, startTime, (long) (time * timeCoef));
                    }
                };
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }


        @Override
        public Stream<NumassFrame> getFrames() {
            return Stream.empty();
        }
    }
}
