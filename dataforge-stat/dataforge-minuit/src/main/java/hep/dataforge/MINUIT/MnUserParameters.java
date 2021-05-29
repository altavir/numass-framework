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

import java.util.List;

/**
 * API class for the user interaction with the parameters. Serves as input to
 * the minimizer as well as output from it; users can interact: fix/release
 * parameters, set values and errors, etc.; parameters can be accessed via their
 * parameter number or via their user-specified name.
 *
 * @version $Id$
 * @author Darksnake
 */
public class MnUserParameters {

    private MnUserTransformation theTransformation;

    /**
     * Creates a new instance of MnUserParameters
     */
    public MnUserParameters() {
        theTransformation = new MnUserTransformation();
    }

    /**
     * <p>Constructor for MnUserParameters.</p>
     *
     * @param par an array of double.
     * @param err an array of double.
     */
    public MnUserParameters(double[] par, double[] err) {
        theTransformation = new MnUserTransformation(par, err);
    }

    private MnUserParameters(MnUserParameters other) {
        theTransformation = other.theTransformation.copy();
    }

    /**
     * Add free parameter name, value, error
     * <p>
     * When adding parameters, MINUIT assigns indices to each parameter which
     * will be the same as in the double[] in the
     * MultiFunction.valueOf(). That means the first parameter the user
     * adds gets index 0, the second index 1, and so on. When calculating the
     * function value inside FCN, MINUIT will call
     * MultiFunction.valueOf() with the elements at their respective
     * positions.
     *
     * @param err a double.
     * @param val a double.
     * @param name a {@link java.lang.String} object.
     */
    public void add(String name, double val, double err) {
        theTransformation.add(name, val, err);
    }

    /**
     * Add limited parameter name, value, lower bound, upper bound
     *
     * @param up a double.
     * @param low a double.
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     * @param err a double.
     */
    public void add(String name, double val, double err, double low, double up) {
        theTransformation.add(name, val, err, low, up);
    }

    /**
     * Add const parameter name, value
     *
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     */
    public void add(String name, double val) {
        theTransformation.add(name, val);
    }

    /**
     * <p>copy.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     */
    protected MnUserParameters copy() {
        return new MnUserParameters(this);
    }

    /**
     * <p>error.</p>
     *
     * @param index a int.
     * @return a double.
     */
    public double error(int index) {
        return theTransformation.error(index);
    }

    /**
     * <p>error.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a double.
     */
    public double error(String name) {
        return theTransformation.error(name);
    }

    double[] errors() {
        return theTransformation.errors();
    }

    /// interaction via external number of parameter

    /**
     * Fixes the specified parameter (so that the minimizer will no longer vary
     * it)
     *
     * @param index a int.
     */
    public void fix(int index) {
        theTransformation.fix(index);
    }

    /// interaction via name of parameter

    /**
     * Fixes the specified parameter (so that the minimizer will no longer vary
     * it)
     *
     * @param name a {@link java.lang.String} object.
     */
    public void fix(String name) {
        theTransformation.fix(name);
    }

    /**
     * convert name into external number of parameter
     * @param name
     * @return 
     */
    int index(String name) {
        return theTransformation.index(name);
    }

    /**
     * convert external number into name of parameter
     * @param index
     * @return 
     */
    String name(int index) {
        return theTransformation.name(index);
    }

    /**
     * access to single parameter
     * @param index
     * @return 
     */
    MinuitParameter parameter(int index) {
        return theTransformation.parameter(index);
    }

    /**
     * access to parameters (row-wise)
     * @return 
     */
    List<MinuitParameter> parameters() {
        return theTransformation.parameters();
    }

    /**
     * access to parameters and errors in column-wise representation
     * @return 
     */
    double[] params() {
        return theTransformation.params();
    }

    /**
     * <p>precision.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnMachinePrecision} object.
     */
    public MnMachinePrecision precision() {
        return theTransformation.precision();
    }

    /**
     * Releases the specified parameter (so that the minimizer can vary it)
     *
     * @param index a int.
     */
    public void release(int index) {
        theTransformation.release(index);
    }

    /**
     * Releases the specified parameter (so that the minimizer can vary it)
     *
     * @param name a {@link java.lang.String} object.
     */
    public void release(String name) {
        theTransformation.release(name);
    }

    /**
     * <p>removeLimits.</p>
     *
     * @param index a int.
     */
    public void removeLimits(int index) {
        theTransformation.removeLimits(index);
    }

    /**
     * <p>removeLimits.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void removeLimits(String name) {
        theTransformation.removeLimits(name);
    }

    /**
     * <p>setError.</p>
     *
     * @param index a int.
     * @param err a double.
     */
    public void setError(int index, double err) {
        theTransformation.setError(index, err);
    }

    /**
     * <p>setError.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param err a double.
     */
    public void setError(String name, double err) {
        theTransformation.setError(name, err);
    }

    /**
     * Set the lower and upper bound on the specified variable.
     *
     * @param up a double.
     * @param low a double.
     * @param index a int.
     */
    public void setLimits(int index, double low, double up) {
        theTransformation.setLimits(index, low, up);
    }

    /**
     * Set the lower and upper bound on the specified variable.
     *
     * @param up a double.
     * @param low a double.
     * @param name a {@link java.lang.String} object.
     */
    public void setLimits(String name, double low, double up) {
        theTransformation.setLimits(name, low, up);
    }

    /**
     * <p>setLowerLimit.</p>
     *
     * @param index a int.
     * @param low a double.
     */
    public void setLowerLimit(int index, double low) {
        theTransformation.setLowerLimit(index, low);
    }

    /**
     * <p>setLowerLimit.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param low a double.
     */
    public void setLowerLimit(String name, double low) {
        theTransformation.setLowerLimit(name, low);
    }

    /**
     * <p>setPrecision.</p>
     *
     * @param eps a double.
     */
    public void setPrecision(double eps) {
        theTransformation.setPrecision(eps);
    }

    /**
     * <p>setUpperLimit.</p>
     *
     * @param index a int.
     * @param up a double.
     */
    public void setUpperLimit(int index, double up) {
        theTransformation.setUpperLimit(index, up);
    }

    /**
     * <p>setUpperLimit.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param up a double.
     */
    public void setUpperLimit(String name, double up) {
        theTransformation.setUpperLimit(name, up);
    }

    /**
     * Set the value of parameter. The parameter in question may be variable,
     * fixed, or constant, but must be defined.
     *
     * @param index a int.
     * @param val a double.
     */
    public void setValue(int index, double val) {
        theTransformation.setValue(index, val);
    }

    /**
     * Set the value of parameter. The parameter in question may be variable,
     * fixed, or constant, but must be defined.
     *
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     */
    public void setValue(String name, double val) {
        theTransformation.setValue(name, val);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MnPrint.toString(this);
    }

    MnUserTransformation trafo() {
        return theTransformation;
    }

        /**
         * <p>value.</p>
         *
         * @param index a int.
         * @return a double.
         */
    public double value(int index) {
        return theTransformation.value(index);
    }

        /**
         * <p>value.</p>
         *
         * @param name a {@link java.lang.String} object.
         * @return a double.
         */
    public double value(String name) {
        return theTransformation.value(name);
    }

        /**
         * <p>variableParameters.</p>
         *
         * @return a int.
         */
    public int variableParameters() {
        return theTransformation.variableParameters();
    }
}
