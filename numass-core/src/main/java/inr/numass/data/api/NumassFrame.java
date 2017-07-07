package inr.numass.data.api;

import java.nio.ShortBuffer;
import java.time.Duration;
import java.time.Instant;

/**
 * The continuous frame of digital detector data
 * Created by darksnake on 06-Jul-17.
 */
public class NumassFrame {

    /**
     * The absolute start time of the frame
     */
    private final Instant startTime;

    /**
     * The buffered signal shape in ticks
     */
    private final ShortBuffer signal;

    /**
     * The time interval per tick
     */
    private final Duration tickSize;


    public NumassFrame(Instant startTime, Duration tickSize, ShortBuffer signal) {
        this.startTime = startTime;
        this.signal = signal;
        this.tickSize = tickSize;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public ShortBuffer getSignal() {
        return signal;
    }

    public Duration getTickSize() {
        return tickSize;
    }

    public Duration getLength() {
        return tickSize.multipliedBy(signal.capacity());
    }
}
