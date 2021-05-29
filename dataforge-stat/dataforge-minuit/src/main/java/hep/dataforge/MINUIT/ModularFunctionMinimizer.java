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

/**
 *
 * @version $Id$
 */
abstract class ModularFunctionMinimizer {

    abstract MinimumBuilder builder();

    FunctionMinimum minimize(MultiFunction fcn, MnUserParameterState st, MnStrategy strategy, int maxfcn, double toler, double errorDef, boolean useAnalyticalGradient, boolean checkGradient) {
        MnUserFcn mfcn = new MnUserFcn(fcn, errorDef, st.getTransformation());

        GradientCalculator gc;
        boolean providesAllDerivs = true;
        /*
        * Проверяем в явном виде, что все аналитические производные присутствуют
        * TODO сделать возможность того, что часть производных задается аналитически, а часть численно
        */
        for (int i = 0; i < fcn.getDimension(); i++) {
            if(!fcn.providesDeriv(i))
                providesAllDerivs = false;
        }
        
        if (providesAllDerivs && useAnalyticalGradient) {
            gc = new AnalyticalGradientCalculator(fcn, st.getTransformation(), checkGradient);
        } else {
            gc = new Numerical2PGradientCalculator(mfcn, st.getTransformation(), strategy);
        }

        int npar = st.variableParameters();
        if (maxfcn == 0) {
            maxfcn = 200 + 100 * npar + 5 * npar * npar;
        }
        MinimumSeed mnseeds = seedGenerator().generate(mfcn, gc, st, strategy);

        return minimize(mfcn, gc, mnseeds, strategy, maxfcn, toler);
    }

    FunctionMinimum minimize(MnFcn mfcn, GradientCalculator gc, MinimumSeed seed, MnStrategy strategy, int maxfcn, double toler) {
        return builder().minimum(mfcn, gc, seed, strategy, maxfcn, toler * mfcn.errorDef());
    }

    abstract MinimumSeedGenerator seedGenerator();
}
