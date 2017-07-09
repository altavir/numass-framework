package inr.numass.data.storage;

import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;
import inr.numass.data.NumassProto;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.NumassFrame;
import inr.numass.data.api.NumassPoint;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Created by darksnake on 09.07.2017.
 */
public class ProtoNumassPoint implements NumassPoint {
    private final Envelope envelope;

    NumassProto.Point point;

    public ProtoNumassPoint(Envelope envelope) {
        this.envelope = envelope;
    }

    private NumassProto.Point getPoint() {
        if (point == null) {
            try (InputStream stream = envelope.getData().getStream()) {
                point = NumassProto.Point.parseFrom(stream);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to read point via protbuf");
            }
        }
        return point;
    }

    @Override
    public Stream<NumassBlock> getBlocks() {
        return null;
    }

    @Override
    public Meta meta() {
        return null;
    }

    private class ProtoBlock implements NumassBlock {

        final NumassProto.Point.Channel.Block block;

        private ProtoBlock(NumassProto.Point.Channel.Block block) {
            this.block = block;
        }

        @Override
        public Instant getStartTime() {
            
        }

        @Override
        public Duration getLength() {
            return null;
        }

        @Override
        public Stream<NumassEvent> getEvents() {
            return null;
        }

        @Override
        public Stream<NumassFrame> getFrames() {
            return null;
        }
    }
}
