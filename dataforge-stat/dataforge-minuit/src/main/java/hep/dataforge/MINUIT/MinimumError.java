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
import org.apache.commons.math3.linear.SingularMatrixException;

/**
 * MinimumError keeps the inverse 2nd derivative (inverse Hessian) used for
 * calculating the parameter step size (-V*g) and for the covariance update
 * (ErrorUpdator). The covariance matrix is equal to twice the inverse Hessian.
 *
 * @version $Id$
 */
class MinimumError {
    private boolean theAvailable;
    private double theDCovar;
    private boolean theHesseFailed;
    private boolean theInvertFailed;
    private boolean theMadePosDef;
    private MnAlgebraicSymMatrix theMatrix;
    private boolean thePosDef;
    private boolean theValid;

    MinimumError(int n) {
        theMatrix = new MnAlgebraicSymMatrix(n);
        theDCovar = 1.;
    }

    MinimumError(MnAlgebraicSymMatrix mat, double dcov) {
        theMatrix = mat;
        theDCovar = dcov;
        theValid = true;
        thePosDef = true;
        theAvailable = true;
    }

    MinimumError(MnAlgebraicSymMatrix mat, MnHesseFailed x) {
        theMatrix = mat;
        theDCovar = 1;
        theValid = false;
        thePosDef = false;
        theMadePosDef = false;
        theHesseFailed = true;
        theInvertFailed = false;
        theAvailable = true;
    }

    MinimumError(MnAlgebraicSymMatrix mat, MnMadePosDef x) {
        theMatrix = mat;
        theDCovar = 1.;
        theValid = false;
        thePosDef = false;
        theMadePosDef = true;
        theHesseFailed = false;
        theInvertFailed = false;
        theAvailable = true;
    }

    MinimumError(MnAlgebraicSymMatrix mat, MnInvertFailed x) {
        theMatrix = mat;
        theDCovar = 1.;
        theValid = false;
        thePosDef = true;
        theMadePosDef = false;
        theHesseFailed = false;
        theInvertFailed = true;
        theAvailable = true;
    }

    MinimumError(MnAlgebraicSymMatrix mat, MnNotPosDef x) {
        theMatrix = mat;
        theDCovar = 1.;
        theValid = false;
        thePosDef = false;
        theMadePosDef = false;
        theHesseFailed = false;
        theInvertFailed = false;
        theAvailable = true;
    }

    double dcovar() {
        return theDCovar;
    }

    boolean hesseFailed() {
        return theHesseFailed;
    }

    MnAlgebraicSymMatrix hessian() {
        try {
            MnAlgebraicSymMatrix tmp = theMatrix.copy();
            tmp.invert();
            return tmp;
        } catch (SingularMatrixException x) {

            MINUITPlugin.logStatic("BasicMinimumError inversion fails; return diagonal matrix.");
            MnAlgebraicSymMatrix tmp = new MnAlgebraicSymMatrix(theMatrix.nrow());
            for (int i = 0; i < theMatrix.nrow(); i++) {
                tmp.set(i, i, 1. / theMatrix.get(i, i));
            }
            return tmp;
        }
    }

    MnAlgebraicSymMatrix invHessian() {
        return theMatrix;
    }

    boolean invertFailed() {
        return theInvertFailed;
    }

    boolean isAccurate() {
        return theDCovar < 0.1;
    }

    boolean isAvailable() {
        return theAvailable;
    }

    boolean isMadePosDef() {
        return theMadePosDef;
    }

    boolean isPosDef() {
        return thePosDef;
    }

    boolean isValid() {
        return theValid;
    }

    MnAlgebraicSymMatrix matrix() {
        return MnUtils.mul(theMatrix, 2);
    }

    static class MnHesseFailed {
    }

    static class MnInvertFailed {
    }

    static class MnMadePosDef {
    }

    static class MnNotPosDef {
    }
}
