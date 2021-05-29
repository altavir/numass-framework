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
 * <p>MinosError class.</p>
 *
 * @author Darksnake
 * @version $Id$
 */
public class MinosError {
    private MnCross theLower;
    private double theMinValue;
    private int theParameter;
    private MnCross theUpper;

    MinosError() {
        theUpper = new MnCross();
        theLower = new MnCross();
    }

    MinosError(int par, double min, MnCross low, MnCross up) {
        theParameter = par;
        theMinValue = min;
        theUpper = up;
        theLower = low;
    }

    /**
     * <p>atLowerLimit.</p>
     *
     * @return a boolean.
     */
    public boolean atLowerLimit() {
        return theLower.atLimit();
    }

    /**
     * <p>atLowerMaxFcn.</p>
     *
     * @return a boolean.
     */
    public boolean atLowerMaxFcn() {
        return theLower.atMaxFcn();
    }

    /**
     * <p>atUpperLimit.</p>
     *
     * @return a boolean.
     */
    public boolean atUpperLimit() {
        return theUpper.atLimit();
    }

    /**
     * <p>atUpperMaxFcn.</p>
     *
     * @return a boolean.
     */
    public boolean atUpperMaxFcn() {
        return theUpper.atMaxFcn();
    }

    /**
     * <p>isValid.</p>
     *
     * @return a boolean.
     */
    public boolean isValid() {
        return theLower.isValid() && theUpper.isValid();
    }

    /**
     * <p>lower.</p>
     *
     * @return a double.
     */
    public double lower() {
        return -1. * lowerState().error(parameter()) * (1. + theLower.value());
    }

    /**
     * <p>lowerNewMin.</p>
     *
     * @return a boolean.
     */
    public boolean lowerNewMin() {
        return theLower.newMinimum();
    }

    /**
     * <p>lowerState.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState lowerState() {
        return theLower.state();
    }

    /**
     * <p>lowerValid.</p>
     *
     * @return a boolean.
     */
    public boolean lowerValid() {
        return theLower.isValid();
    }

    /**
     * <p>min.</p>
     *
     * @return a double.
     */
    public double min() {
        return theMinValue;
    }

    /**
     * <p>nfcn.</p>
     *
     * @return a int.
     */
    public int nfcn() {
        return theUpper.nfcn() + theLower.nfcn();
    }

    /**
     * <p>parameter.</p>
     *
     * @return a int.
     */
    public int parameter() {
        return theParameter;
    }

    /**
     * <p>range.</p>
     *
     * @return
     */
    public Range range() {
        return new Range(lower(), upper());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MnPrint.toString(this);
    }

    /**
     * <p>upper.</p>
     *
     * @return a double.
     */
    public double upper() {
        return upperState().error(parameter()) * (1. + theUpper.value());
    }

    /**
     * <p>upperNewMin.</p>
     *
     * @return a boolean.
     */
    public boolean upperNewMin() {
        return theUpper.newMinimum();
    }

    /**
     * <p>upperState.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState upperState() {
        return theUpper.state();
    }

    /**
     * <p>upperValid.</p>
     *
     * @return a boolean.
     */
    public boolean upperValid() {
        return theUpper.isValid();
    }
}
