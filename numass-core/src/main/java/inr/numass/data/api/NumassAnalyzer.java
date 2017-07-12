package inr.numass.data.api;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.*;
import hep.dataforge.values.Values;
import inr.numass.data.analyzers.SmartAnalyzer;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static hep.dataforge.tables.XYAdapter.*;

/**
 * A general raw data analysis utility. Could have different implementations
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassAnalyzer {

    static Table getSpectrum(NumassBlock block, Meta config) {
        TableFormat format = new TableFormatBuilder()
                .addNumber("channel", X_VALUE_KEY)
                .addNumber("count")
                .addNumber(COUNT_RATE_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
                .updateMeta(metaBuilder -> metaBuilder.setNode("config", config))
                .build();
        NavigableMap<Short, AtomicLong> map = new TreeMap<>();
        new SmartAnalyzer().getEventStream(block, config).forEach(event -> {
            if (map.containsKey(event.getChanel())) {
                map.get(event.getChanel()).incrementAndGet();
            } else {
                map.put(event.getChanel(), new AtomicLong(1));
            }
        });
        return new ListTable.Builder(format)
                .rows(map.entrySet().stream()
                        .map(entry ->
                                new ValueMap(format.namesAsArray(),
                                        entry.getKey(),
                                        entry.getValue(),
                                        entry.getValue().get() / block.getLength().toMillis() * 1000,
                                        Math.sqrt(entry.getValue().get()) / block.getLength().toMillis() * 1000
                                )
                        )
                ).build();
    }

    String COUNT_RATE_KEY = "cr";
    String COUNT_RATE_ERROR_KEY = "crErr";

    /**
     * Perform analysis on block. The values for count rate, its error and point length in nanos must
     * exist, but occasionally additional values could also be presented.
     *
     * @param block
     * @return
     */
    Values analyze(NumassBlock block, Meta config);

    /**
     * Return unsorted stream of events including events from frames
     *
     * @param block
     * @return
     */
    Stream<NumassEvent> getEventStream(NumassBlock block, Meta config);

    /**
     * Analyze the whole set. And return results as a table
     *
     * @param set
     * @param config
     * @return
     */
    Table analyze(NumassSet set, Meta config);

}
