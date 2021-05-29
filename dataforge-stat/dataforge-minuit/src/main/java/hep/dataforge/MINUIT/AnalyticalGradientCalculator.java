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
package hep.dataforge.MINUIT;

import hep.dataforge.maths.functions.MultiFunction;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @version $Id$
 */
class AnalyticalGradientCalculator implements GradientCalculator {
    private MultiFunction function;
    private boolean theCheckGradient;
    private MnUserTransformation theTransformation;

    AnalyticalGradientCalculator(MultiFunction fcn, MnUserTransformation state, boolean checkGradient) {
        function = fcn;
        theTransformation = state;
        theCheckGradient = checkGradient;
    }

    boolean checkGradient() {
        return theCheckGradient;
    }

    /** {@inheritDoc} */
    @Override
    public FunctionGradient gradient(MinimumParameters par) {
//      double[] grad = theGradCalc.gradientValue(theTransformation.andThen(par.vec()).data());
        double[] point = theTransformation.transform(par.vec()).toArray();
        if (function.getDimension() != theTransformation.parameters().size()) {
            throw new IllegalArgumentException("Invalid parameter size");
        }

        RealVector v = new ArrayRealVector(par.vec().getDimension());
        for (int i = 0; i < par.vec().getDimension(); i++) {
            int ext = theTransformation.extOfInt(i);
            if (theTransformation.parameter(ext).hasLimits()) {
                double dd = theTransformation.dInt2Ext(i, par.vec().getEntry(i));
                v.setEntry(i, dd * function.derivValue(ext, point));
            } else {
                v.setEntry(i, function.derivValue(ext, point));
            }
        }

        return new FunctionGradient(v);
    }

    /** {@inheritDoc} */
    @Override
    public FunctionGradient gradient(MinimumParameters par, FunctionGradient grad) {
        return gradient(par);
    }
}
