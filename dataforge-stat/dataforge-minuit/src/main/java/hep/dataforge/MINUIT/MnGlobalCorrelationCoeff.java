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

import org.apache.commons.math3.linear.SingularMatrixException;

/**
 * <p>MnGlobalCorrelationCoeff class.</p>
 *
 * @version $Id$
 * @author Darksnake
 */
public class MnGlobalCorrelationCoeff {

    private double[] theGlobalCC;
    private boolean theValid;

    MnGlobalCorrelationCoeff() {
        theGlobalCC = new double[0];
    }

    MnGlobalCorrelationCoeff(MnAlgebraicSymMatrix cov) {
        try {
            MnAlgebraicSymMatrix inv = cov.copy();
            inv.invert();
            theGlobalCC = new double[cov.nrow()];
            for (int i = 0; i < cov.nrow(); i++) {
                double denom = inv.get(i, i) * cov.get(i, i);
                if (denom < 1. && denom > 0.) {
                    theGlobalCC[i] = 0;
                } else {
                    theGlobalCC[i] = Math.sqrt(1. - 1. / denom);
                }
            }
            theValid = true;
        } catch (SingularMatrixException x) {
            theValid = false;
            theGlobalCC = new double[0];
        }
    }

    /**
     * <p>globalCC.</p>
     *
     * @return an array of double.
     */
    public double[] globalCC() {
        return theGlobalCC;
    }

    /**
     * <p>isValid.</p>
     *
     * @return a boolean.
     */
    public boolean isValid() {
        return theValid;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MnPrint.toString(this);
    }
}
