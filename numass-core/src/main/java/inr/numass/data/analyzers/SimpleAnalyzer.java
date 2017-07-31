package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.api.SignalProcessor;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

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

    public Stream<NumassEvent> getEventStream(NumassBlock block, int loChannel, int upChannel) {
        return getEvents(block, Meta.empty()).filter(it -> it.getChanel() >= loChannel && it.getChanel() < upChannel);
    }

    @Override
    public Values analyze(NumassBlock block, Meta config) {
        int loChannel = config.getInt("window.lo", 0);
        int upChannel = config.getInt("window.up", Integer.MAX_VALUE);
        long count = getEventStream(block, loChannel, upChannel).count();
        double countRate = (double) count / block.getLength().toMillis() * 1000;
        double countRateError = Math.sqrt((double) count) / block.getLength().toMillis() * 1000;

        if (block instanceof NumassPoint) {
            return ValueMap.of(NAME_LIST_WITH_HV,
                    ((NumassPoint) block).getVoltage(),
                    block.getLength().toNanos(),
                    count,
                    countRate,
                    countRateError,
                    new int[]{loChannel, upChannel},
                    block.getStartTime());
        } else {
            return ValueMap.of(NAME_LIST,
                    block.getLength().toNanos(),
                    count,
                    countRate,
                    countRateError,
                    new int[]{loChannel, upChannel},
                    block.getStartTime());
        }
    }


}
