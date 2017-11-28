package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.TableFormatBuilder;
import inr.numass.data.api.*;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.stream.Stream;

import static hep.dataforge.tables.Adapters.*;
import static inr.numass.data.api.NumassPoint.HV_KEY;

/**
 * Created by darksnake on 11.07.2017.
 */
public abstract class AbstractAnalyzer implements NumassAnalyzer {
    public static String WINDOW_KEY = "window";
    public static String TIME_KEY = "timestamp";

    public static String[] NAME_LIST = {LENGTH_KEY, COUNT_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, WINDOW_KEY, TIME_KEY};
//    public static String[] NAME_LIST_WITH_HV = {HV_KEY, LENGTH_KEY, COUNT_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, WINDOW_KEY, TIME_KEY};
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
        int loChannel = config.getInt("window.lo", 0);
        int upChannel = config.getInt("window.up", Integer.MAX_VALUE);
        Stream<NumassEvent> res = getAllEvents(block).filter(event -> {
            short channel = event.getChanel();
            return channel >= loChannel && channel < upChannel;
        });
        if (config.getBoolean("sort", false)) {
            res = res.sorted(Comparator.comparing(NumassEvent::getTimeOffset));
        }
        return res;
    }

    protected Stream<NumassEvent> getAllEvents(NumassBlock block) {
        if (block.getFrames().count() == 0) {
            return block.getEvents();
        } else if (getProcessor() == null) {
            throw new IllegalArgumentException("Signal processor needed to analyze frames");
        } else {
            return Stream.concat(block.getEvents(), block.getFrames().flatMap(getProcessor()::analyze));
        }
    }

    /**
     * Get table format for summary table
     *
     * @param config
     * @return
     */
    protected TableFormat getTableFormat(Meta config) {
        return new TableFormatBuilder()
                .addNumber(HV_KEY, X_VALUE_KEY)
                .addNumber(LENGTH_KEY)
                .addNumber(COUNT_KEY)
                .addNumber(COUNT_RATE_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
                .addColumn(WINDOW_KEY)
                .addTime()
                .build();
    }


    @Override
    public Table analyzeSet(NumassSet set, Meta config) {
        TableFormat format = getTableFormat(config);

        return new ListTable.Builder(format)
                .rows(set.getPoints().map(point -> analyzePoint(point, config)))
                .build();
    }

    @Nullable
    public SignalProcessor getProcessor() {
        return processor;
    }
}
