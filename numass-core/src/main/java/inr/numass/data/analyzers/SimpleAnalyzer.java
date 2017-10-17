package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.SignalProcessor;
import org.jetbrains.annotations.Nullable;

/**
 * A simple event counter
 * Created by darksnake on 07.07.2017.
 */
public class SimpleAnalyzer extends AbstractAnalyzer {

    public SimpleAnalyzer(@Nullable SignalProcessor processor) {
        super(processor);
    }

    public SimpleAnalyzer() {
    }


    @Override
    public Values analyze(NumassBlock block, Meta config) {
        int loChannel = config.getInt("window.lo", 0);
        int upChannel = config.getInt("window.up", Integer.MAX_VALUE);
        long count = getEvents(block, config).count();
        double length = (double) block.getLength().toNanos() / 1e9;
        double countRate = (double) count / length;
        double countRateError = Math.sqrt((double) count) / length;

        return ValueMap.of(NAME_LIST,
                length,
                count,
                countRate,
                countRateError,
                new Integer[]{loChannel, upChannel},
                block.getStartTime());
    }


}
