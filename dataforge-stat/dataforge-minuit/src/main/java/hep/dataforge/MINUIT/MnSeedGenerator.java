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

import hep.dataforge.stat.fit.MINUITPlugin;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @version $Id$
 */
class MnSeedGenerator implements MinimumSeedGenerator {

        /** {@inheritDoc} */
    @Override
    public MinimumSeed generate(MnFcn fcn, GradientCalculator gc, MnUserParameterState st, MnStrategy stra) {
        int n = st.variableParameters();
        MnMachinePrecision prec = st.precision();

        // initial starting values
        RealVector x = new ArrayRealVector(n);
        for (int i = 0; i < n; i++) {
            x.setEntry(i, st.intParameters().get(i));
        }
        double fcnmin = fcn.value(x);
        MinimumParameters pa = new MinimumParameters(x, fcnmin);

        FunctionGradient dgrad;
        if (gc instanceof AnalyticalGradientCalculator) {
            InitialGradientCalculator igc = new InitialGradientCalculator(fcn, st.getTransformation(), stra);
            FunctionGradient tmp = igc.gradient(pa);
            FunctionGradient grd = gc.gradient(pa);
            dgrad = new FunctionGradient(grd.getGradient(), tmp.getGradientDerivative(), tmp.getStep());

            if (((AnalyticalGradientCalculator) gc).checkGradient()) {
                boolean good = true;
                HessianGradientCalculator hgc = new HessianGradientCalculator(fcn, st.getTransformation(), new MnStrategy(2));
                Pair<FunctionGradient, RealVector> hgrd = hgc.deltaGradient(pa, dgrad);
                for (int i = 0; i < n; i++) {
                    double provided = grd.getGradient().getEntry(i);
                    double calculated = hgrd.getFirst().getGradient().getEntry(i);
                    double delta = hgrd.getSecond().getEntry(i);
                    if (Math.abs(calculated - provided) > delta) {
                        MINUITPlugin.logStatic(""
                                + "gradient discrepancy of external parameter \"%d\" "
                                + "(internal parameter \"%d\") too large. Expected: \"%f\", provided: \"%f\"",
                                st.getTransformation().extOfInt(i),i,provided,calculated);

//                        
//                        MINUITPlugin.logStatic("gradient discrepancy of external parameter "
//                                + st.getTransformation().extOfInt(i) 
//                                + " (internal parameter " + i + ") too large.");
//                        good = false;
                    }
                }
                if (!good) {
                    MINUITPlugin.logStatic("Minuit does not accept user specified gradient.");
//               assert(good);
                }
            }
        } else {
            dgrad = gc.gradient(pa);
        }
        MnAlgebraicSymMatrix mat = new MnAlgebraicSymMatrix(n);
        double dcovar = 1.;
        if (st.hasCovariance()) {
            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    mat.set(i, j, st.intCovariance().get(i, j));
                }
            }
            dcovar = 0.;
        } else {
            for (int i = 0; i < n; i++) {
                mat.set(i, i, (Math.abs(dgrad.getGradientDerivative().getEntry(i)) > prec.eps2() ? 1. / dgrad.getGradientDerivative().getEntry(i) : 1.));
            }
        }
        MinimumError err = new MinimumError(mat, dcovar);
        double edm = new VariableMetricEDMEstimator().estimate(dgrad, err);
        MinimumState state = new MinimumState(pa, err, dgrad, edm, fcn.numOfCalls());


        if (NegativeG2LineSearch.hasNegativeG2(dgrad, prec)) {
            if (gc instanceof AnalyticalGradientCalculator) {
                Numerical2PGradientCalculator ngc = new Numerical2PGradientCalculator(fcn, st.getTransformation(), stra);
                state = NegativeG2LineSearch.search(fcn, state, ngc, prec);
            } else {
                state = NegativeG2LineSearch.search(fcn, state, gc, prec);
            }
        }

        if (stra.strategy() == 2 && !st.hasCovariance()) {
            //calculate full 2nd derivative
            MinimumState tmp = new MnHesse(stra).calculate(fcn, state, st.getTransformation(), 0);
            return new MinimumSeed(tmp, st.getTransformation());
        }

        return new MinimumSeed(state, st.getTransformation());
    }
}
