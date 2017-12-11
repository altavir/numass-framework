package inr.numass.data.api;

import hep.dataforge.meta.Metoid;
import hep.dataforge.values.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassPoint extends Metoid, NumassBlock {

    String START_TIME_KEY = "start";
    String LENGTH_KEY = "length";
    String HV_KEY = "voltage";
    String INDEX_KEY = "index";


    Stream<NumassBlock> getBlocks();

    /**
     * Get the voltage setting for the point
     *
     * @return
     */
    default double getVoltage() {
        return getMeta().getDouble(HV_KEY, 0);
    }

    /**
     * Get the index for this point in the set
     * @return
     */
    default int getIndex() {
        return getMeta().getInt(INDEX_KEY, -1);
    }

    /**
     * Get the first block if it exists. Throw runtime exception otherwise.
     *
     * @return
     */
    default NumassBlock getFirstBlock() {
        return getBlocks().findFirst().orElseThrow(() -> new RuntimeException("The point is empty"));
    }

    /**
     * Get the starting time from meta or from first block
     *
     * @return
     */
    @Override
    default Instant getStartTime() {
        return getMeta().optValue(START_TIME_KEY).map(Value::timeValue).orElseGet(() -> getFirstBlock().getStartTime());
    }

    /**
     * Get the length key of meta or calculate length as a sum of block lengths. The latter could be a bit slow
     *
     * @return
     */
    @Override
    default Duration getLength() {
        return Duration.ofNanos(
                getMeta().optValue(LENGTH_KEY).map(Value::longValue)
                        .orElseGet(() -> getBlocks().mapToLong(it -> it.getLength().toNanos()).sum())
        );
    }

    /**
     * Get all events it all blocks as a single sequence
     * <p>
     * Some performance analysis of different stream concatenation approaches is given here: https://www.techempower.com/blog/2016/10/19/efficient-multiple-stream-concatenation-in-java/
     * </p>
     *
     * @return
     */
    @Override
    default Stream<NumassEvent> getEvents() {
        return getBlocks().flatMap(NumassBlock::getEvents);
    }

    /**
     * Get all frames in all blocks as a single sequence
     *
     * @return
     */
    @Override
    default Stream<NumassFrame> getFrames() {
        return getBlocks().flatMap(NumassBlock::getFrames);
    }
}
