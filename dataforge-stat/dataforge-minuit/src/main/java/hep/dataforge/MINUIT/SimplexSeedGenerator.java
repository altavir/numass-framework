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
 *
 * @version $Id$
 */
class SimplexSeedGenerator implements MinimumSeedGenerator {

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
        InitialGradientCalculator igc = new InitialGradientCalculator(fcn, st.getTransformation(), stra);
        FunctionGradient dgrad = igc.gradient(pa);
        MnAlgebraicSymMatrix mat = new MnAlgebraicSymMatrix(n);
        double dcovar = 1.;
        for (int i = 0; i < n; i++) {
            mat.set(i, i, Math.abs(dgrad.getGradientDerivative().getEntry(i)) > prec.eps2() ? 1. / dgrad.getGradientDerivative().getEntry(i) : 1.);
        }
        MinimumError err = new MinimumError(mat, dcovar);
        double edm = new VariableMetricEDMEstimator().estimate(dgrad, err);
        MinimumState state = new MinimumState(pa, err, dgrad, edm, fcn.numOfCalls());

        return new MinimumSeed(state, st.getTransformation());
    }
}
