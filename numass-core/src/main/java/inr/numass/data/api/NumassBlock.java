package inr.numass.data.api;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * A single continuous measurement block. The block can contain both isolated events and signal frames
 * <p>
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassBlock {
    /**
     * The absolute start time of the block
     * @return
     */
    Instant getStartTime();

    /**
     * The length of the block
     * @return
     */
    Duration getLength();

    /**
     * Stream of isolated events. Could be empty
     * @return
     */
    Stream<NumassEvent> getEvents();

    /**
     * Stream of frames. Could be empty
     * @return
     */
    Stream<NumassFrame> getFrames();
}
