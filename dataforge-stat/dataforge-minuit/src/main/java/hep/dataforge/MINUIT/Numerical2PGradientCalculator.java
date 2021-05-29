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

import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @version $Id$
 */
class Numerical2PGradientCalculator implements GradientCalculator {
    private MnFcn theFcn;
    private MnStrategy theStrategy;
    private MnUserTransformation theTransformation;

    Numerical2PGradientCalculator(MnFcn fcn, MnUserTransformation par, MnStrategy stra) {
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
        if (!par.isValid()) {
            throw new IllegalArgumentException("Parameters are invalid");
        }

        RealVector x = par.vec().copy();

        double fcnmin = par.fval();
        double dfmin = 8. * precision().eps2() * (Math.abs(fcnmin) + theFcn.errorDef());
        double vrysml = 8. * precision().eps() * precision().eps();

        int n = x.getDimension();
        RealVector grd = gradient.getGradient().copy();
        RealVector g2 = gradient.getGradientDerivative().copy();
        RealVector gstep = gradient.getStep().copy();
        for (int i = 0; i < n; i++) {
            double xtf = x.getEntry(i);
            double epspri = precision().eps2() + Math.abs(grd.getEntry(i) * precision().eps2());
            double stepb4 = 0.;
            for (int j = 0; j < ncycle(); j++) {
                double optstp = Math.sqrt(dfmin / (Math.abs(g2.getEntry(i)) + epspri));
                double step = Math.max(optstp, Math.abs(0.1 * gstep.getEntry(i)));

                if (trafo().parameter(trafo().extOfInt(i)).hasLimits()) {
                    if (step > 0.5) {
                        step = 0.5;
                    }
                }
                double stpmax = 10. * Math.abs(gstep.getEntry(i));
                if (step > stpmax) {
                    step = stpmax;
                }

                double stpmin = Math.max(vrysml, 8. * Math.abs(precision().eps2() * x.getEntry(i)));
                if (step < stpmin) {
                    step = stpmin;
                }
                if (Math.abs((step - stepb4) / step) < stepTolerance()) {
                    break;
                }
                gstep.setEntry(i, step);
                stepb4 = step;

                x.setEntry(i, xtf + step);
                double fs1 = theFcn.value(x);
                x.setEntry(i, xtf - step);
                double fs2 = theFcn.value(x);
                x.setEntry(i, xtf);

                double grdb4 = grd.getEntry(i);

                grd.setEntry(i, 0.5 * (fs1 - fs2) / step);
                g2.setEntry(i, (fs1 + fs2 - 2. * fcnmin) / step / step);

                if (Math.abs(grdb4 - grd.getEntry(i)) / (Math.abs(grd.getEntry(i)) + dfmin / step) < gradTolerance()) {
                    break;
                }
            }

        }
        return new FunctionGradient(grd, g2, gstep);

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
