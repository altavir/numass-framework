package inr.numass.data.api;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/**
 * A simple in-memory implementation of block of events. No frames are allowed
 * Created by darksnake on 08.07.2017.
 */
public class SimpleBlock implements NumassBlock, Serializable {
    private final Instant startTime;
    private final Duration length;
    private final List<NumassEvent> events;

    public SimpleBlock(Instant startTime, Duration length, List<NumassEvent> events) {
        this.startTime = startTime;
        this.length = length;
        this.events = events;
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
        return events.stream();
    }

    @Override
    public Stream<NumassFrame> getFrames() {
        return Stream.empty();
    }
}
