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
package hep.dataforge.stat.fit;

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.maths.NamedMatrix;
import hep.dataforge.stat.parametric.DerivativeCalculator;
import hep.dataforge.stat.parametric.ParametricUtils;
import hep.dataforge.stat.parametric.ParametricValue;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Work in progress
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class Hessian{
    
    /**
     * <p>getHessian.</p>
     *
     * @param function a {@link hep.dataforge.stat.parametric.ParametricValue} object.
     * @param set a {@link hep.dataforge.stat.fit.ParamSet} object.
     * @param fitPars an array of {@link java.lang.String} objects.
     * @return a {@link hep.dataforge.maths.NamedMatrix} object.
     */
    public static NamedMatrix getHessian(ParametricValue function, ParamSet set, String[] fitPars){
        if(!set.getNames().contains(fitPars)){
            throw new NameNotFoundException();
        }
        
        int dim = fitPars.length;
        
        RealMatrix res = new Array2DRowRealMatrix(dim, dim);
        
        for (int i = 0; i < dim; i++) {
            for (int j = i; j < dim; j++) {
                UnivariateFunction func = ParametricUtils.getNamedProjectionDerivative(function, fitPars[i], fitPars[j], set);
                double value = DerivativeCalculator.calculateDerivative(func, set.getDouble(fitPars[i]), set.getError(fitPars[j])/2);
                res.setEntry(i, j, value);
                res.setEntry(j, i, value);                
            }
        }
        
        return new NamedMatrix(fitPars, res);
    }

    
}
