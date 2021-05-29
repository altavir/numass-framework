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

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * In case that one of the components of the second derivative g2 calculated by
 * the numerical gradient calculator is negative, a 1dim line search in the
 * direction of that component is done in order to find a better position where
 * g2 is again positive.
 *
 * @version $Id$
 */
abstract class NegativeG2LineSearch {

    static boolean hasNegativeG2(FunctionGradient grad, MnMachinePrecision prec) {
        for (int i = 0; i < grad.getGradient().getDimension(); i++) {
            if (grad.getGradientDerivative().getEntry(i) < prec.eps2()) {
                return true;
            }
        }
        
        return false;
    }

    static MinimumState search(MnFcn fcn, MinimumState st, GradientCalculator gc, MnMachinePrecision prec) {
        boolean negG2 = hasNegativeG2(st.gradient(), prec);
        if (!negG2) {
            return st;
        }

        int n = st.parameters().vec().getDimension();
        FunctionGradient dgrad = st.gradient();
        MinimumParameters pa = st.parameters();
        boolean iterate = false;
        int iter = 0;
        do {
            iterate = false;
            for (int i = 0; i < n; i++) {
                if (dgrad.getGradientDerivative().getEntry(i) < prec.eps2()) {
                    // do line search if second derivative negative
                    RealVector step = new ArrayRealVector(n);
                    step.setEntry(i, dgrad.getStep().getEntry(i) * dgrad.getGradient().getEntry(i));
                    if (Math.abs(dgrad.getGradient().getEntry(i)) > prec.eps2()) {
                        step.setEntry(i, step.getEntry(i) * (-1. / Math.abs(dgrad.getGradient().getEntry(i))));
                    }
                    double gdel = step.getEntry(i) * dgrad.getGradient().getEntry(i);
                    MnParabolaPoint pp = MnLineSearch.search(fcn, pa, step, gdel, prec);
                    step = MnUtils.mul(step, pp.x());
                    pa = new MinimumParameters(MnUtils.add(pa.vec(), step), pp.y());
                    dgrad = gc.gradient(pa, dgrad);
                    iterate = true;
                    break;
                }
            }
        } while (iter++ < 2 * n && iterate);

        MnAlgebraicSymMatrix mat = new MnAlgebraicSymMatrix(n);
        for (int i = 0; i < n; i++) {
            mat.set(i, i, Math.abs(dgrad.getGradientDerivative().getEntry(i)) > prec.eps2() ? 1. / dgrad.getGradientDerivative().getEntry(i) : 1.);
        }

        MinimumError err = new MinimumError(mat, 1.);
        double edm = new VariableMetricEDMEstimator().estimate(dgrad, err);

        return new MinimumState(pa, err, dgrad, edm, fcn.numOfCalls());
    }
}
