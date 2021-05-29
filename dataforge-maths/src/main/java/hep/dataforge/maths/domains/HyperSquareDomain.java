/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.maths.domains;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * <p>HyperSquareDomain class.</p>
 *
 * @author Alexander Nozik
 */
public class HyperSquareDomain implements Domain {

    private Double[] lower;
    private Double[] upper;

    public HyperSquareDomain(int num) {
        this.lower = new Double[num];
        this.upper = new Double[num];
        for (int i = 0; i < num; i++) {
            this.lower[i] = Double.NEGATIVE_INFINITY;
            this.upper[i] = Double.POSITIVE_INFINITY;
        }
    }

    public HyperSquareDomain(Double[] lower, Double[] upper) {
        if (lower.length != upper.length) {
            throw new IllegalArgumentException();
        }
        this.lower = lower;
        this.upper = upper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(RealVector point) {
        for (int i = 0; i < point.getDimension(); i++) {
            if ((point.getEntry(i) < this.lower[i]) || (point.getEntry(i) > this.upper[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(double[] point) {
        return this.contains(new ArrayRealVector(point));
    }

    public void fix(int num, double value) {
        this.setDomain(num, value, value);
    }

    @Override
    public int getDimension() {
        return this.lower.length;
    }

    @Override
    public Double getLowerBound(int num, RealVector point) {
        return this.lower[num];
    }

    @Override
    public Double getLowerBound(int num) {
        return this.lower[num];
    }

    @Override
    public Double getUpperBound(int num, RealVector point) {
        return this.upper[num];
    }

    @Override
    public Double getUpperBound(int num) {
        return this.upper[num];
    }

    @Override
    public RealVector nearestInDomain(RealVector point) {
        RealVector res = point.copy();
        for (int i = 0; i < point.getDimension(); i++) {
            if (point.getEntry(i) < this.lower[i]) {
                res.setEntry(i, lower[i]);
            }
            if (point.getEntry(i) > this.upper[i]) {
                res.setEntry(i, upper[i]);
            }
        }
        return res;
    }

    public void setDomain(int num, Double lower, Double upper) {
        if (num >= this.getDimension()) {
            throw new IllegalArgumentException();
        }
        if (lower > upper) {
            throw new IllegalArgumentException("\'lower\' argument should be lower.");
        }
        this.lower[num] = lower;
        this.upper[num] = upper;
    }

    public void setLowerBorders(Double[] lower) {
        if (lower.length != this.getDimension()) {
            throw new IllegalArgumentException();
        }
        this.lower = lower;
    }

    public void setUpperBorders(Double[] upper) {
        if (upper.length != this.getDimension()) {
            throw new IllegalArgumentException();
        }
        this.upper = upper;
    }

    @Override
    public double volume() {
        double res = 1;
        for (int i = 0; i < this.getDimension(); i++) {
            if (this.lower[i].isInfinite() || this.upper[i].isInfinite()) {
                return Double.POSITIVE_INFINITY;
            }
            if (upper[i] > lower[i]) {
                res *= upper[i] - lower[i];
            }
        }
        return res;
    }
}
