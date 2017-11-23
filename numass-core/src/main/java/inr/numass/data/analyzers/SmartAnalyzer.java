package inr.numass.data.analyzers;

import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueType;
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
            if(config.hasValue("t0")||config.hasValue("t0")){
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

    private double estimateCountRate(NumassBlock block) {
        return (double) block.getEvents().count() / block.getLength().toMillis() * 1000;
    }

    @Override
    public Stream<NumassEvent> getEvents(NumassBlock block, Meta config) {
        return getAnalyzer(config).getEvents(block, config);
    }

    @ValueDef(name = "t0", type = ValueType.NUMBER, info = "Constant t0 cut")
    @ValueDef(name = "t0.crFraction", type = ValueType.NUMBER, info = "The relative fraction of events that should be removed by time cut")
    @ValueDef(name = "t0.min", type = ValueType.NUMBER, def = "0", info = "Minimal t0")
    private int getT0(NumassBlock block, Meta meta) {
        if (meta.hasValue("t0")) {
            return meta.getInt("t0");
        } else if (meta.hasMeta("t0")) {
            double fraction = meta.getDouble("t0.crFraction");
            double cr = estimateCountRate(block);
            if (cr < meta.getDouble("t0.minCR", 0)) {
                return 0;
            } else {
                return (int) Math.max(-1e9 / cr * Math.log(1d - fraction), meta.getDouble("t0.min", 0));
            }
        } else {
            return 0;
        }

    }

    @Override
    protected TableFormat getTableFormat(Meta config) {
        if (config.hasValue(TimeAnalyzer.T0_KEY) || config.hasMeta(TimeAnalyzer.T0_KEY)) {
            return timeAnalyzer.getTableFormat(config);
        }
        return super.getTableFormat(config);
    }
}
