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


/**
 *
 * @version $Id$
 */
class CombinedMinimumBuilder implements MinimumBuilder {
    private SimplexMinimizer theSimplexMinimizer = new SimplexMinimizer();
    private VariableMetricMinimizer theVMMinimizer = new VariableMetricMinimizer();

    /** {@inheritDoc} */
    @Override
    public FunctionMinimum minimum(MnFcn fcn, GradientCalculator gc, MinimumSeed seed, MnStrategy strategy, int maxfcn, double toler) {
        FunctionMinimum min = theVMMinimizer.minimize(fcn, gc, seed, strategy, maxfcn, toler);

        if (!min.isValid()) {
            MINUITPlugin.logStatic("CombinedMinimumBuilder: migrad method fails, will try with simplex method first.");
            MnStrategy str = new MnStrategy(2);
            FunctionMinimum min1 = theSimplexMinimizer.minimize(fcn, gc, seed, str, maxfcn, toler);
            if (!min1.isValid()) {
                MINUITPlugin.logStatic("CombinedMinimumBuilder: both migrad and simplex method fail.");
                return min1;
            }
            MinimumSeed seed1 = theVMMinimizer.seedGenerator().generate(fcn, gc, min1.userState(), str);

            FunctionMinimum min2 = theVMMinimizer.minimize(fcn, gc, seed1, str, maxfcn, toler);
            if (!min2.isValid()) {
                MINUITPlugin.logStatic("CombinedMinimumBuilder: both migrad and method fails also at 2nd attempt.");
                MINUITPlugin.logStatic("CombinedMinimumBuilder: return simplex minimum.");
                return min1;
            }

            return min2;
        }
        return min;
    }
}
