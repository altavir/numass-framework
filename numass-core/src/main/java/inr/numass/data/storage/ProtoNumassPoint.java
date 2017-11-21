package inr.numass.data.storage;

import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;
import inr.numass.data.NumassProto;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.NumassFrame;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.legacy.NumassFileEnvelope;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Protobuf based numass point
 * Created by darksnake on 09.07.2017.
 */
public class ProtoNumassPoint implements NumassPoint {
    public static ProtoNumassPoint readFile(Path path) {
        return new ProtoNumassPoint(NumassFileEnvelope.open(path, true));
    }


    private final Envelope envelope;

    public ProtoNumassPoint(Envelope envelope) {
        this.envelope = envelope;
    }

    private NumassProto.Point getPoint() {
        try (InputStream stream = envelope.getData().getStream()) {
            return NumassProto.Point.parseFrom(stream);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read point via protobuf");
        }
    }

    @Override
    public Stream<NumassBlock> getBlocks() {
        return getPoint().getChannelsList().stream()
                .flatMap(channel ->
                        channel.getBlocksList().stream()
                                .map(block -> new ProtoBlock((int) channel.getNum(), block))
                                .sorted(Comparator.comparing(ProtoBlock::getStartTime))
                );
    }

    @Override
    public Meta getMeta() {
        return envelope.getMeta();
    }

    public static Instant ofEpochNanos(long nanos) {
        long seconds = Math.floorDiv(nanos, (int) 1e9);
        int reminder = (int) (nanos % 1e9);
        return Instant.ofEpochSecond(seconds, reminder);
    }

    private class ProtoBlock implements NumassBlock {

        final int channel;
        final NumassProto.Point.Channel.Block block;

        private ProtoBlock(int channel, NumassProto.Point.Channel.Block block) {
            this.channel = channel;
            this.block = block;
        }

        @Override
        public Instant getStartTime() {
            return ofEpochNanos(block.getTime());
        }

        @Override
        public Duration getLength() {
            return Duration.ofNanos((long) (getMeta().getInt("b_size") / getMeta().getInt("sample_freq") * 1e9));
        }

        @Override
        public Stream<NumassEvent> getEvents() {
            Instant blockTime = getStartTime();
            if (block.hasEvents()) {
                NumassProto.Point.Channel.Block.Events events = block.getEvents();
                return IntStream.range(0, events.getTimesCount()).mapToObj(i ->
                        new NumassEvent((short) events.getAmplitudes(i), blockTime, events.getTimes(i))
                );
            } else {
                return Stream.empty();
            }
        }

        @Override
        public Stream<NumassFrame> getFrames() {
            Duration tickSize = Duration.ofNanos((long) (1e9 / getMeta().getInt("params.sample_freq")));
            return block.getFramesList().stream().map(frame -> {
                Instant time = getStartTime().plusNanos(frame.getTime());
                ByteBuffer data = frame.getData().asReadOnlyByteBuffer();
                return new NumassFrame(time, tickSize, data.asShortBuffer());
            });
        }
    }
}
