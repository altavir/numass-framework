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
class MinimumParameters {
    private double theFVal;
    private boolean theHasStep;
    private RealVector theParameters;
    private RealVector theStepSize;
    private boolean theValid;

    MinimumParameters(int n) {
        theParameters = new ArrayRealVector(n);
        theStepSize = new ArrayRealVector(n);
    }

    MinimumParameters(RealVector avec, double fval) {
        theParameters = avec;
        theStepSize = new ArrayRealVector(avec.getDimension());
        theFVal = fval;
        theValid = true;
    }

    MinimumParameters(RealVector avec, RealVector dirin, double fval) {
        theParameters = avec;
        theStepSize = dirin;
        theFVal = fval;
        theValid = true;
        theHasStep = true;
    }

    RealVector dirin() {
        return theStepSize;
    }

    double fval() {
        return theFVal;
    }

    boolean hasStepSize() {
        return theHasStep;
    }

    boolean isValid() {
        return theValid;
    }

    RealVector vec() {
        return theParameters;
    }
}
