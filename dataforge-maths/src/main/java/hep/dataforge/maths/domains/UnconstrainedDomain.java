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

import hep.dataforge.exceptions.NotDefinedException;
import org.apache.commons.math3.linear.RealVector;

/**
 * <p>UnconstrainedDomain class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class UnconstrainedDomain implements Domain {

    private final int dimension;

    /**
     * <p>Constructor for UnconstrainedDomain.</p>
     *
     * @param dimension a int.
     */
    public UnconstrainedDomain(int dimension) {
        this.dimension = dimension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(RealVector point) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(double[] point) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDimension() {
        return dimension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double getLowerBound(int num, RealVector point) {
        return Double.NEGATIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double getLowerBound(int num) throws NotDefinedException {
        return Double.NEGATIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double getUpperBound(int num, RealVector point) {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double getUpperBound(int num) throws NotDefinedException {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RealVector nearestInDomain(RealVector point) {
        return point;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double volume() {
        return Double.POSITIVE_INFINITY;
    }
}
