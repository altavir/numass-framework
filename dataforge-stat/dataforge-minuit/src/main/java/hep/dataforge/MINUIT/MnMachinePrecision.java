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

/**
 * Determines the relative floating point arithmetic precision. The
 * setPrecision() method can be used to override Minuit's own determination,
 * when the user knows that the {FCN} function value is not calculated to the
 * nominal machine accuracy.
 *
 * @version $Id$
 * @author Darksnake
 */
public final class MnMachinePrecision {
    private double theEpsMa2;
    private double theEpsMac;

    MnMachinePrecision() {
        setPrecision(4.0E-7);

        double epstry = 0.5;
        double one = 1.0;
        for (int i = 0; i < 100; i++) {
            epstry *= 0.5;
            double epsp1 = one + epstry;
            double epsbak = epsp1 - one;
            if (epsbak < epstry) {
                setPrecision(8. * epstry);
                break;
            }
        }
    }

    /**
     * eps returns the smallest possible number so that 1.+eps > 1.
     * @return 
     */
    double eps() {
        return theEpsMac;
    }

    /**
     * eps2 returns 2*sqrt(eps)
     * @return 
     */
    double eps2() {
        return theEpsMa2;
    }

    /**
     * override Minuit's own determination
     *
     * @param prec a double.
     */
    public void setPrecision(double prec) {
        theEpsMac = prec;
        theEpsMa2 = 2. * Math.sqrt(theEpsMac);
    }
}
