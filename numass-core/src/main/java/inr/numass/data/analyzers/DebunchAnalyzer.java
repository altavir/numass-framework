package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.SignalProcessor;
import org.jetbrains.annotations.Nullable;

/**
 * Block analyzer that can perform debunching
 * Created by darksnake on 11.07.2017.
 */
public class DebunchAnalyzer extends AbstractAnalyzer {
    public DebunchAnalyzer(@Nullable SignalProcessor processor) {
        super(processor);
    }

    public DebunchAnalyzer() {
    }

    @Override
    public Values analyze(NumassBlock block, Meta config) {
        throw new UnsupportedOperationException("TODO");
    }
}
