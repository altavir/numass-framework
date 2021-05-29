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
 * MinimumState keeps the information (position, gradient, 2nd deriv, etc) after
 * one minimization step (usually in MinimumBuilder).
 *
 * @version $Id$
 */
class MinimumState {
    private double theEDM;
    private MinimumError theError;
    private FunctionGradient theGradient;
    private int theNFcn;
    private MinimumParameters theParameters;

    MinimumState(int n) {
        theParameters = new MinimumParameters(n);
        theError = new MinimumError(n);
        theGradient = new FunctionGradient(n);
    }

    MinimumState(MinimumParameters states, MinimumError err, FunctionGradient grad, double edm, int nfcn) {
        theParameters = states;
        theError = err;
        theGradient = grad;
        theEDM = edm;
        theNFcn = nfcn;
    }

    MinimumState(MinimumParameters states, double edm, int nfcn) {
        theParameters = states;
        theError = new MinimumError(states.vec().getDimension());
        theGradient = new FunctionGradient(states.vec().getDimension());
        theEDM = edm;
        theNFcn = nfcn;
    }

    double edm() {
        return theEDM;
    }

    MinimumError error() {
        return theError;
    }

    double fval() {
        return theParameters.fval();
    }

    FunctionGradient gradient() {
        return theGradient;
    }

    boolean hasCovariance() {
        return theError.isAvailable();
    }

    boolean hasParameters() {
        return theParameters.isValid();
    }

    boolean isValid() {
        if (hasParameters() && hasCovariance()) {
            return parameters().isValid() && error().isValid();
        } else if (hasParameters()) {
            return parameters().isValid();
        } else {
            return false;
        }
    }

    int nfcn() {
        return theNFcn;
    }

    MinimumParameters parameters() {
        return theParameters;
    }

    int size() {
        return theParameters.vec().getDimension();
    }

    RealVector vec() {
        return theParameters.vec();
    }
}
