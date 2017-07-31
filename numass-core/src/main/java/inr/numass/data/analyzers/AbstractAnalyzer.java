package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.TableFormatBuilder;
import inr.numass.data.api.*;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

import static hep.dataforge.tables.XYAdapter.*;
import static inr.numass.data.api.NumassPoint.HV_KEY;

/**
 * Created by darksnake on 11.07.2017.
 */
public abstract class AbstractAnalyzer implements NumassAnalyzer {
    public static String[] NAME_LIST = {LENGTH_KEY, COUNT_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, "window", "timestamp"};
    public static String[] NAME_LIST_WITH_HV = {HV_KEY, LENGTH_KEY, COUNT_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, "window", "timestamp"};
    @Nullable
    private final SignalProcessor processor;

    public AbstractAnalyzer(@Nullable SignalProcessor processor) {
        this.processor = processor;
    }

    public AbstractAnalyzer() {
        this.processor = null;
    }

    /**
     * Return unsorted stream of events including events from frames.
     * In theory, events after processing could be unsorted due to mixture of frames and events.
     * In practice usually block have either frame or events, but not both.
     *
     * @param block
     * @return
     */
    public Stream<NumassEvent> getEvents(NumassBlock block, Meta config) {
        if (block.getFrames().count() == 0) {
            return block.getEvents();
        } else if (getProcessor() == null) {
            throw new IllegalArgumentException("Signal processor needed to analyze frames");
        } else {
            return Stream.concat(block.getEvents(), block.getFrames().flatMap(getProcessor()::analyze));
        }
    }


    @Override
    public Table analyze(NumassSet set, Meta config) {
        TableFormat format = new TableFormatBuilder()
                .addNumber(HV_KEY, X_VALUE_KEY)
                .addNumber(LENGTH_KEY)
                .addNumber(COUNT_KEY)
                .addNumber(COUNT_RATE_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
                .addColumn("window")
                .addTime()
                .build();

        return new ListTable.Builder(format)
                .rows(set.getPoints().map(point -> analyze(point, config)))
                .build();
    }

    @Nullable
    public SignalProcessor getProcessor() {
        return processor;
    }
}
