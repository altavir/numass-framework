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
class FunctionGradient {
    private boolean theAnalytical;
    private RealVector theG2ndDerivative;
    private RealVector theGStepSize;
    private RealVector theGradient;
    private boolean theValid;

    FunctionGradient(int n) {
        theGradient = new ArrayRealVector(n);
        theG2ndDerivative = new ArrayRealVector(n);
        theGStepSize = new ArrayRealVector(n);
    }

    FunctionGradient(RealVector grd) {
        theGradient = grd;
        theG2ndDerivative = new ArrayRealVector(grd.getDimension());
        theGStepSize = new ArrayRealVector(grd.getDimension());
        theValid = true;
        theAnalytical = true;
    }

    FunctionGradient(RealVector grd, RealVector g2, RealVector gstep) {
        theGradient = grd;
        theG2ndDerivative = g2;
        theGStepSize = gstep;
        theValid = true;
        theAnalytical = false;
    }

    RealVector getGradient() {
        return theGradient;
    }

    RealVector getGradientDerivative() {
        return theG2ndDerivative;
    }

    RealVector getStep() {
        return theGStepSize;
    }

    boolean isAnalytical() {
        return theAnalytical;
    }

    boolean isValid() {
        return theValid;
    }
}
