package hep.dataforge.maths.domains;

import hep.dataforge.exceptions.NotDefinedException;
import org.apache.commons.math3.linear.RealVector;

public abstract class UnivariateDomain implements Domain {

    public abstract boolean contains(double d);

    @Override
    public boolean contains(RealVector point) {
        return contains(point.getEntry(0));
    }

    @Override
    public RealVector nearestInDomain(RealVector point) {
        return null;
    }

    public abstract Double getLower();

    public abstract Double getUpper();

    @Override
    public Double getLowerBound(int num, RealVector point) {
        return getLower();
    }

    @Override
    public Double getUpperBound(int num, RealVector point) {
        return getUpper();
    }

    @Override
    public Double getLowerBound(int num) throws NotDefinedException {
        return getLower();
    }

    @Override
    public Double getUpperBound(int num) throws NotDefinedException {
        return getUpper();
    }

    @Override
    public int getDimension() {
        return 1;
    }
}
