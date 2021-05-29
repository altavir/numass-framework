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

import java.util.ArrayList;
import java.util.List;


/**
 * Scans the values of FCN as a function of one parameter and retains the best
 * function and parameter values found
 *
 * @version $Id$
 */
class MnParameterScan {
    private double theAmin;

    private MultiFunction theFCN;
    private MnUserParameters theParameters;

    MnParameterScan(MultiFunction fcn, MnUserParameters par) {
        theFCN = fcn;
        theParameters = par;
        theAmin = fcn.value(par.params());
    }

    MnParameterScan(MultiFunction fcn, MnUserParameters par, double fval) {
        theFCN = fcn;
        theParameters = par;
        theAmin = fval;
    }

    double fval() {
        return theAmin;
    }

    MnUserParameters parameters() {
        return theParameters;
    }

    List<Range> scan(int par) {
        return scan(par, 41);
    }

    List<Range> scan(int par, int maxsteps) {
        return scan(par, maxsteps, 0, 0);
    }

    /**
     * returns pairs of (x,y) points, x=parameter value, y=function value of FCN
     * @param high
     * @return 
     */
    List<Range> scan(int par, int maxsteps, double low, double high) {
        if (maxsteps > 101) {
            maxsteps = 101;
        }
        List<Range> result = new ArrayList<>(maxsteps + 1);
        double[] params = theParameters.params();
        result.add(new Range(params[par], theAmin));

        if (low > high) {
            return result;
        }
        if (maxsteps < 2) {
            return result;
        }

        if (low == 0. && high == 0.) {
            low = params[par] - 2. * theParameters.error(par);
            high = params[par] + 2. * theParameters.error(par);
        }

        if (low == 0. && high == 0. && theParameters.parameter(par).hasLimits()) {
            if (theParameters.parameter(par).hasLowerLimit()) {
                low = theParameters.parameter(par).lowerLimit();
            }
            if (theParameters.parameter(par).hasUpperLimit()) {
                high = theParameters.parameter(par).upperLimit();
            }
        }

        if (theParameters.parameter(par).hasLimits()) {
            if (theParameters.parameter(par).hasLowerLimit()) {
                low = Math.max(low, theParameters.parameter(par).lowerLimit());
            }
            if (theParameters.parameter(par).hasUpperLimit()) {
                high = Math.min(high, theParameters.parameter(par).upperLimit());
            }
        }

        double x0 = low;
        double stp = (high - low) / (maxsteps - 1.);
        for (int i = 0; i < maxsteps; i++) {
            params[par] = x0 + ((double) i) * stp;
            double fval = theFCN.value(params);
            if (fval < theAmin) {
                theParameters.setValue(par, params[par]);
                theAmin = fval;
            }
            result.add(new Range(params[par], fval));
        }

        return result;
    }
}
