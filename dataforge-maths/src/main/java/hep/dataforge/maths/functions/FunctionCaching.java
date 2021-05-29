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

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import static hep.dataforge.maths.GridCalculator.getUniformUnivariateGrid;

/**
 * <p>FunctionCaching class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class FunctionCaching {
    
    /**
     * <p>cacheUnivariateFunction.</p>
     *
     * @param a a double.
     * @param b a double.
     * @param numCachePoints a int.
     * @param func a {@link UnivariateFunction} object.
     * @return a {@link org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction} object.
     */
    public static PolynomialSplineFunction cacheUnivariateFunction(double a, double b, int numCachePoints, UnivariateFunction func){
        assert func != null;
        assert a > Double.NEGATIVE_INFINITY;
        double[] grid = getUniformUnivariateGrid(a, b, numCachePoints);
        double[] vals = new double[grid.length];
        
        for (int i = 0; i < vals.length; i++) {
            vals[i] = func.value(grid[i]);
            
        }
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction interpolated = interpolator.interpolate(grid, vals);
        return interpolated;
        
    }    
}
