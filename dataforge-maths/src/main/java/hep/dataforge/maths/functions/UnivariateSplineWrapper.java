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
package hep.dataforge.maths.functions;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.OutOfRangeException;

/**
 * A wrapper function for spline including valuew outside the spline region
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class UnivariateSplineWrapper implements UnivariateDifferentiableFunction {

    private final double outOfRegionValue;
    PolynomialSplineFunction source;

    /**
     * <p>Constructor for UnivariateSplineWrapper.</p>
     *
     * @param source a {@link org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction} object.
     * @param outOfRegionValue a double.
     */
    public UnivariateSplineWrapper(PolynomialSplineFunction source, double outOfRegionValue) {
        this.source = source;
        this.outOfRegionValue = outOfRegionValue;
    }

    /**
     * <p>Constructor for UnivariateSplineWrapper.</p>
     *
     * @param source a {@link org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction} object.
     */
    public UnivariateSplineWrapper(PolynomialSplineFunction source) {
        this.source = source;
        this.outOfRegionValue = 0d;
    }

    
    /** {@inheritDoc} */
    @Override
    public DerivativeStructure value(DerivativeStructure t) throws DimensionMismatchException {
        try {
            return source.value(t);
        } catch (OutOfRangeException ex) {
            return new DerivativeStructure(t.getFreeParameters(), t.getOrder(), outOfRegionValue);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double value(double x) {
        try {
            return source.value(x);
        } catch (OutOfRangeException ex) {
            return outOfRegionValue;
        }
    }
}
