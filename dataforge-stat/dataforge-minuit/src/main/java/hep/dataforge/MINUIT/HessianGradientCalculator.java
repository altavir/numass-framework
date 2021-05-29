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
import org.apache.commons.math3.util.Pair;

/**
 *
 * @version $Id$
 */
class HessianGradientCalculator implements GradientCalculator {
    private MnFcn theFcn;
    private MnStrategy theStrategy;
    private MnUserTransformation theTransformation;

    HessianGradientCalculator(MnFcn fcn, MnUserTransformation par, MnStrategy stra) {
        theFcn = fcn;
        theTransformation = par;
        theStrategy = stra;
    }

    Pair<FunctionGradient, RealVector> deltaGradient(MinimumParameters par, FunctionGradient gradient) {
        if (!par.isValid()) {
            throw new IllegalArgumentException("parameters are invalid");
        }

        RealVector x = par.vec().copy();
        RealVector grd = gradient.getGradient().copy();
        RealVector g2 = gradient.getGradientDerivative();
        RealVector gstep = gradient.getStep();

        double fcnmin = par.fval();
        //   std::cout<<"fval: "<<fcnmin<<std::endl;

        double dfmin = 4. * precision().eps2() * (Math.abs(fcnmin) + theFcn.errorDef());

        int n = x.getDimension();
        RealVector dgrd = new ArrayRealVector(n);

        // initial starting values
        for (int i = 0; i < n; i++) {
            double xtf = x.getEntry(i);
            double dmin = 4. * precision().eps2() * (xtf + precision().eps2());
            double epspri = precision().eps2() + Math.abs(grd.getEntry(i) * precision().eps2());
            double optstp = Math.sqrt(dfmin / (Math.abs(g2.getEntry(i)) + epspri));
            double d = 0.2 * Math.abs(gstep.getEntry(i));
            if (d > optstp) {
                d = optstp;
            }
            if (d < dmin) {
                d = dmin;
            }
            double chgold = 10000.;
            double dgmin = 0.;
            double grdold = 0.;
            double grdnew = 0.;
            for (int j = 0; j < ncycle(); j++) {
                x.setEntry(i, xtf + d);
                double fs1 = theFcn.value(x);
                x.setEntry(i, xtf - d);
                double fs2 = theFcn.value(x);
                x.setEntry(i, xtf);
                //       double sag = 0.5*(fs1+fs2-2.*fcnmin);
                grdold = grd.getEntry(i);
                grdnew = (fs1 - fs2) / (2. * d);
                dgmin = precision().eps() * (Math.abs(fs1) + Math.abs(fs2)) / d;
                if (Math.abs(grdnew) < precision().eps()) {
                    break;
                }
                double change = Math.abs((grdold - grdnew) / grdnew);
                if (change > chgold && j > 1) {
                    break;
                }
                chgold = change;
                grd.setEntry(i, grdnew);
                if (change < 0.05) {
                    break;
                }
                if (Math.abs(grdold - grdnew) < dgmin) {
                    break;
                }
                if (d < dmin) {
                    break;
                }
                d *= 0.2;
            }
            dgrd.setEntry(i, Math.max(dgmin, Math.abs(grdold - grdnew)));
        }

        return new Pair<>(new FunctionGradient(grd, g2, gstep), dgrd);
    }

    MnFcn fcn() {
        return theFcn;
    }

    double gradTolerance() {
        return strategy().gradientTolerance();
    }

    /** {@inheritDoc} */
    @Override
    public FunctionGradient gradient(MinimumParameters par) {
        InitialGradientCalculator gc = new InitialGradientCalculator(theFcn, theTransformation, theStrategy);
        FunctionGradient gra = gc.gradient(par);
        return gradient(par, gra);
    }

    /** {@inheritDoc} */
    @Override
    public FunctionGradient gradient(MinimumParameters par, FunctionGradient gradient) {
        return deltaGradient(par, gradient).getFirst();
    }

    int ncycle() {
        return strategy().hessianGradientNCycles();
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
