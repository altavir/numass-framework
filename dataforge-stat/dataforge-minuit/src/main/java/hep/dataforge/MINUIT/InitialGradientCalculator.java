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
 * Calculating derivatives via finite differences
 * @version $Id$
 */
class InitialGradientCalculator {

    private MnFcn theFcn;
    private MnStrategy theStrategy;
    private MnUserTransformation theTransformation;

    InitialGradientCalculator(MnFcn fcn, MnUserTransformation par, MnStrategy stra) {
        theFcn = fcn;
        theTransformation = par;
        theStrategy = stra;
    }

    MnFcn fcn() {
        return theFcn;
    }

    double gradTolerance() {
        return strategy().gradientTolerance();
    }

    FunctionGradient gradient(MinimumParameters par) {
        if (!par.isValid()) {
            throw new IllegalArgumentException("Parameters are invalid");
        }

        int n = trafo().variableParameters();
        if (n != par.vec().getDimension()) {
            throw new IllegalArgumentException("Parameters have invalid size");
        }
        RealVector gr = new ArrayRealVector(n);
        RealVector gr2 = new ArrayRealVector(n);
        RealVector gst = new ArrayRealVector(n);

        // initial starting values
        for (int i = 0; i < n; i++) {
            int exOfIn = trafo().extOfInt(i);

            double var = par.vec().getEntry(i);//parameter value
            double werr = trafo().parameter(exOfIn).error();//parameter error
            double sav = trafo().int2ext(i, var);//value after transformation
            double sav2 = sav + werr;//value after transfomation + error
            if (trafo().parameter(exOfIn).hasLimits()) {
                if (trafo().parameter(exOfIn).hasUpperLimit()
                        && sav2 > trafo().parameter(exOfIn).upperLimit()) {
                    sav2 = trafo().parameter(exOfIn).upperLimit();
                }
            }
            double var2 = trafo().ext2int(exOfIn, sav2);
            double vplu = var2 - var;
            sav2 = sav - werr;
            if (trafo().parameter(exOfIn).hasLimits()) {
                if (trafo().parameter(exOfIn).hasLowerLimit()
                        && sav2 < trafo().parameter(exOfIn).lowerLimit()) {
                    sav2 = trafo().parameter(exOfIn).lowerLimit();
                }
            }
            var2 = trafo().ext2int(exOfIn, sav2);
            double vmin = var2 - var;
            double dirin = 0.5 * (Math.abs(vplu) + Math.abs(vmin));
            double g2 = 2.0 * theFcn.errorDef() / (dirin * dirin);
            double gsmin = 8. * precision().eps2() * (Math.abs(var) + precision().eps2());
            double gstep = Math.max(gsmin, 0.1 * dirin);
            double grd = g2 * dirin;
            if (trafo().parameter(exOfIn).hasLimits()) {
                if (gstep > 0.5) {
                    gstep = 0.5;
                }
            }
            gr.setEntry(i, grd);
            gr2.setEntry(i, g2);
            gst.setEntry(i, gstep);
        }

        return new FunctionGradient(gr, gr2, gst);
    }

    FunctionGradient gradient(MinimumParameters par, FunctionGradient gra) {
        return gradient(par);
    }

    int ncycle() {
        return strategy().gradientNCycles();
    }

    MnMachinePrecision precision() {
        return theTransformation.precision();
    }

    double stepTolerance() {
        return strategy().gradientStepTolerance();
    }

    MnStrategy strategy() {
        return theStrategy;
    }

    MnUserTransformation trafo() {
        return theTransformation;
    }
}
