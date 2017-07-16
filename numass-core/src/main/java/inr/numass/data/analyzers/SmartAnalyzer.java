package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.SignalProcessor;
import org.jetbrains.annotations.Nullable;

/**
 * An analyzer dispatcher which uses different analyzer for different meta
 * Created by darksnake on 11.07.2017.
 */
public class SmartAnalyzer extends AbstractAnalyzer {
    private SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
    private DebunchAnalyzer debunchAnalyzer = new DebunchAnalyzer();
    private TimeAnalyzer timeAnalyzer = new TimeAnalyzer();

    public SmartAnalyzer(@Nullable SignalProcessor processor) {
        super(processor);
        this.simpleAnalyzer = new SimpleAnalyzer(processor);
        this.debunchAnalyzer = new DebunchAnalyzer(processor);
        this.timeAnalyzer = new TimeAnalyzer(processor);
    }

    public SmartAnalyzer() {
    }

    @Override
    public Values analyze(NumassBlock block, Meta config) {
        //TODO add result caching
        //TODO do something more... smart... using information from point if block is point
        switch (config.getString("type", "simple")) {
            case "simple":
                return simpleAnalyzer.analyze(block, config);
            case "time":
                return timeAnalyzer.analyze(block, config);
            case "debunch":
                return debunchAnalyzer.analyze(block, config);
            default:
                throw new IllegalArgumentException("Analyzer not found");
        }
    }
}
