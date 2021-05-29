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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * Performs a minimization using the simplex method of Nelder and Mead (ref.
 * Comp. J. 7, 308 (1965)).
 *
 * @version $Id$
 */
class ScanBuilder implements MinimumBuilder {

        /** {@inheritDoc} */
    @Override
    public FunctionMinimum minimum(MnFcn mfcn, GradientCalculator gc, MinimumSeed seed, MnStrategy stra, int maxfcn, double toler) {
        RealVector x = seed.parameters().vec().copy();
        MnUserParameterState upst = new MnUserParameterState(seed.state(), mfcn.errorDef(), seed.trafo());
        MnParameterScan scan = new MnParameterScan(mfcn.fcn(), upst.parameters(), seed.fval());
        double amin = scan.fval();
        int n = seed.trafo().variableParameters();
        RealVector dirin = new ArrayRealVector(n);
        for (int i = 0; i < n; i++) {
            int ext = seed.trafo().extOfInt(i);
            scan.scan(ext);
            if (scan.fval() < amin) {
                amin = scan.fval();
                x.setEntry(i, seed.trafo().ext2int(ext, scan.parameters().value(ext)));
            }
            dirin.setEntry(i, Math.sqrt(2. * mfcn.errorDef() * seed.error().invHessian().get(i, i)));
        }

        MinimumParameters mp = new MinimumParameters(x, dirin, amin);
        MinimumState st = new MinimumState(mp, 0., mfcn.numOfCalls());

        List<MinimumState> states = new ArrayList<>(1);
        states.add(st);
        return new FunctionMinimum(seed, states, mfcn.errorDef());
    }
}
