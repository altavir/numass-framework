package hep.dataforge.stat.fit;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.names.NameList;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueFactory;
import kotlin.Pair;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of univariate estimates with different confidence levels
 * Created by darksnake on 16-Aug-16.
 */
public class BasicIntervalEstimate implements IntervalEstimate {
    private final double cl;
    private final Set<ParameterRange> ranges = new HashSet<>();

    public BasicIntervalEstimate(double cl) {
        this.cl = cl;
    }

    public BasicIntervalEstimate put(String parName, double cl, Value lower, Value upper) {
        this.ranges.add(new ParameterRange(parName, cl, lower, upper));
        return this;
    }

    public BasicIntervalEstimate put(String parName, double cl, Object lower, Object upper) {
        return this.put(parName, cl, ValueFactory.of(lower), ValueFactory.of(upper));
    }


    @Override
    public NameList getNames() {
        return new NameList(ranges.stream().map(range -> range.parName).distinct().toArray(String[]::new));
    }

    @Override
    public void print(PrintWriter out) {
        out.printf("%s\t%s\t%-8s\t%-8s%n", "name", "CL ", "Lower", "Upper");
        ranges.stream().sorted().forEach(range -> {
            out.printf("%s\t%2.2g%%\t%8.8g\t%8.8g%n", range.parName, range.cl,
                    range.lower.getDouble(), range.upper.getDouble());
        });
        out.println();
    }

    @Override
    public Pair<Value, Value> getInterval(String parName) {
        return ranges.stream().filter(range -> range.cl == this.cl && range.parName.equals(parName))
                .map(range -> new Pair<>(range.lower, range.upper))
                .findFirst().orElseThrow(NotDefinedException::new);
    }

    @Override
    public double getCL() {
        return cl;
    }

    /**
     * The copy of this estimate with different CL base
     *
     * @param cl
     * @return
     */
    private BasicIntervalEstimate withCL(double cl) {
        BasicIntervalEstimate res = new BasicIntervalEstimate(cl);
        res.ranges.addAll(this.ranges);
        return res;
    }

    private class ParameterRange implements Serializable, Comparable<ParameterRange> {
        String parName;
        double cl;
        Value lower;
        Value upper;

        public ParameterRange(String parName, double cl, Value lower, Value upper) {
            this.parName = parName;
            this.cl = cl;
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public int compareTo(ParameterRange o) {
            return this.parName.compareTo(o.parName) * 10 + Double.compare(this.cl, o.cl);
        }
    }
}
