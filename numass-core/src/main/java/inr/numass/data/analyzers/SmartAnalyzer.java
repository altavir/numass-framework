package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassAnalyzer;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.SignalProcessor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

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

    private NumassAnalyzer getAnalyzer(Meta config){
        if (config.hasValue("type")) {
            switch (config.getString("type")) {
                case "simple":
                    return simpleAnalyzer;
                case "time":
                    return timeAnalyzer;
                case "debunch":
                    return debunchAnalyzer;
                default:
                    throw new IllegalArgumentException("Analyzer not found");
            }
        } else {
            if(config.hasValue("t0")||config.hasMeta("t0")){
                return timeAnalyzer;
            } else {
                return simpleAnalyzer;
            }
        }
    }

    @Override
    public Values analyze(NumassBlock block, Meta config) {
        NumassAnalyzer analyzer = getAnalyzer(config);
        Map<String, Value> map = analyzer.analyze(block, config).asMap();
        map.putIfAbsent(TimeAnalyzer.T0_KEY, Value.of(0d));
        return new ValueMap(map);
    }

    @Override
    public Stream<NumassEvent> getEvents(NumassBlock block, Meta config) {
        return getAnalyzer(config).getEvents(block, config);
    }

    @Override
    protected TableFormat getTableFormat(Meta config) {
        if (config.hasValue(TimeAnalyzer.T0_KEY) || config.hasMeta(TimeAnalyzer.T0_KEY)) {
            return timeAnalyzer.getTableFormat(config);
        }
        return super.getTableFormat(config);
    }
}
