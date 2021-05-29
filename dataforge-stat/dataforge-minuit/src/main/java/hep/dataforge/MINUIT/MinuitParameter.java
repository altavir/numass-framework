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
 *
 * @version $Id$
 */
class MinuitParameter {

    private boolean theConst;
    private double theError;
    private boolean theFix;
    private boolean theLoLimValid;
    private double theLoLimit;
    private String theName;
    private int theNum;
    private boolean theUpLimValid;
    private double theUpLimit;
    private double theValue;

    /**
     * constructor for constant parameter
     *
     * @param num a int.
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     */
    public MinuitParameter(int num, String name, double val) {
        theNum = num;
        theValue = val;
        theConst = true;
        theName = name;
    }

    /**
     * constructor for standard parameter
     *
     * @param num a int.
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     * @param err a double.
     */
    public MinuitParameter(int num, String name, double val, double err) {
        theNum = num;
        theValue = val;
        theError = err;
        theName = name;
    }

    /**
     * constructor for limited parameter
     *
     * @param num a int.
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     * @param err a double.
     * @param min a double.
     * @param max a double.
     */
    public MinuitParameter(int num, String name, double val, double err, double min, double max) {
        theNum = num;
        theValue = val;
        theError = err;
        theLoLimit = min;
        theUpLimit = max;
        theLoLimValid = true;
        theUpLimValid = true;
        if (min == max) {
            throw new IllegalArgumentException("min == max");
        }
        if (min > max) {
            theLoLimit = max;
            theUpLimit = min;
        }
        theName = name;
    }

    private MinuitParameter(MinuitParameter other) {
        theNum = other.theNum;
        theName = other.theName;
        theValue = other.theValue;
        theError = other.theError;
        theConst = other.theConst;
        theFix = other.theFix;
        theLoLimit = other.theLoLimit;
        theUpLimit = other.theUpLimit;
        theLoLimValid = other.theLoLimValid;
        theUpLimValid = other.theUpLimValid;
    }

    /**
     * <p>copy.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MinuitParameter} object.
     */
    protected MinuitParameter copy() {
        return new MinuitParameter(this);
    }
    /**
     * <p>error.</p>
     *
     * @return a double.
     */
    public double error() {
        return theError;
    }

    /**
     * <p>fix.</p>
     */
    public void fix() {
        theFix = true;
    }

    /**
     * <p>hasLimits.</p>
     *
     * @return a boolean.
     */
    public boolean hasLimits() {
        return theLoLimValid || theUpLimValid;
    }

    /**
     * <p>hasLowerLimit.</p>
     *
     * @return a boolean.
     */
    public boolean hasLowerLimit() {
        return theLoLimValid;
    }
    /**
     * <p>hasUpperLimit.</p>
     *
     * @return a boolean.
     */
    public boolean hasUpperLimit() {
        return theUpLimValid;
    }

    //state of parameter (fixed/const/limited)
    /**
     * <p>isConst.</p>
     *
     * @return a boolean.
     */
    public boolean isConst() {
        return theConst;
    }

    /**
     * <p>isFixed.</p>
     *
     * @return a boolean.
     */
    public boolean isFixed() {
        return theFix;
    }

    /**
     * <p>lowerLimit.</p>
     *
     * @return a double.
     */
    public double lowerLimit() {
        return theLoLimit;
    }

    /**
     * <p>name.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String name() {
        return theName;
    }

    //access methods
    /**
     * <p>number.</p>
     *
     * @return a int.
     */
    public int number() {
        return theNum;
    }

    /**
     * <p>release.</p>
     */
    public void release() {
        theFix = false;
    }

    /**
     * <p>removeLimits.</p>
     */
    public void removeLimits() {
        theLoLimit = 0.;
        theUpLimit = 0.;
        theLoLimValid = false;
        theUpLimValid = false;
    }
    /**
     * <p>setError.</p>
     *
     * @param err a double.
     */
    public void setError(double err) {
        theError = err;
        theConst = false;
    }

    /**
     * <p>setLimits.</p>
     *
     * @param low a double.
     * @param up a double.
     */
    public void setLimits(double low, double up) {
        if (low == up) {
            throw new IllegalArgumentException("min == max");
        }
        theLoLimit = low;
        theUpLimit = up;
        theLoLimValid = true;
        theUpLimValid = true;
        if (low > up) {
            theLoLimit = up;
            theUpLimit = low;
        }
    }

    /**
     * <p>setLowerLimit.</p>
     *
     * @param low a double.
     */
    public void setLowerLimit(double low) {
        theLoLimit = low;
        theUpLimit = 0.;
        theLoLimValid = true;
        theUpLimValid = false;
    }

    /**
     * <p>setUpperLimit.</p>
     *
     * @param up a double.
     */
    public void setUpperLimit(double up) {
        theLoLimit = 0.;
        theUpLimit = up;
        theLoLimValid = false;
        theUpLimValid = true;
    }

    //interaction
    /**
     * <p>setValue.</p>
     *
     * @param val a double.
     */
    public void setValue(double val) {
        theValue = val;
    }

    /**
     * <p>upperLimit.</p>
     *
     * @return a double.
     */
    public double upperLimit() {
        return theUpLimit;
    }

    /**
     * <p>value.</p>
     *
     * @return a double.
     */
    public double value() {
        return theValue;
    }
}
