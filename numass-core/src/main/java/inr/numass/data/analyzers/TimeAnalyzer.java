package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.SignalProcessor;
import org.jetbrains.annotations.Nullable;

/**
 * Created by darksnake on 11.07.2017.
 */
public class TimeAnalyzer extends AbstractAnalyzer {

    public TimeAnalyzer(@Nullable SignalProcessor processor) {
        super(processor);
    }

    public TimeAnalyzer() {
    }

    @Override
    public Values analyze(NumassBlock block, Meta config) {
        throw new UnsupportedOperationException("TODO");
    }
}
