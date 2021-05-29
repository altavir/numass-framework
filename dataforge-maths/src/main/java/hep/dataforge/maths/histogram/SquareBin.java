package hep.dataforge.maths.histogram;

import hep.dataforge.maths.domains.HyperSquareDomain;
import hep.dataforge.names.NameList;
import hep.dataforge.names.NameSetContainer;
import hep.dataforge.names.NamesUtils;
import hep.dataforge.utils.ArgumentChecker;
import hep.dataforge.values.ValueMap;
import hep.dataforge.values.Values;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by darksnake on 29-Jun-17.
 */
public class SquareBin extends HyperSquareDomain implements Bin, NameSetContainer {

    private final long binId;
    private AtomicLong counter = new AtomicLong(0);
    private NameList names; // optional names for the bin

    /**
     * Create a multivariate bin
     *
     * @param lower
     * @param upper
     */
    public SquareBin(Double[] lower, Double[] upper) {
        super(lower, upper);
        this.binId = Arrays.hashCode(lower);
    }

    /**
     * Create a univariate bin
     *
     * @param lower
     * @param upper
     */
    public SquareBin(Double lower, Double upper) {
        this(new Double[]{lower}, new Double[]{upper});
    }

    @Override
    @NotNull
    public synchronized NameList getNames() {
        if (names == null) {
            names = NamesUtils.generateNames(getDimension());
        }
        return names;
    }

    public void setNames(NameList names) {
        ArgumentChecker.checkEqualDimensions(getDimension(), names.size());
        this.names = names;
    }

    /**
     * Get the lower bound for 0 axis
     *
     * @return
     */
    public Double getLowerBound() {
        return this.getLowerBound(0);
    }

    /**
     * Get the upper bound for 0 axis
     *
     * @return
     */
    public Double getUpperBound() {
        return this.getUpperBound(0);
    }

    @Override
    public long inc() {
        return counter.incrementAndGet();
    }

    @Override
    public long getCount() {
        return counter.get();
    }

    @Override
    public long setCount(long c) {
        return counter.getAndSet(c);
    }

    @Override
    public long getBinID() {
        return binId;
    }

    @Override
    public Values describe() {
        ValueMap.Builder builder = new ValueMap.Builder();
        for (int i = 0; i < getDimension(); i++) {
            String axisName = getNames().get(i);
            Double binStart = getLowerBound(i);
            Double binEnd = getUpperBound(i);
            builder.putValue(axisName, binStart);
            builder.putValue(axisName + ".binEnd", binEnd);
        }
        builder.putValue("count", getCount());
        builder.putValue("id", getBinID());
        return builder.build();
    }
}