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

import java.util.List;

/**
 * Base class for minimizers.
 *
 * @version $Id$
 * @author Darksnake
 */
public abstract class MnApplication {

    static int DEFAULT_MAXFCN = 0;
    static int DEFAULT_STRATEGY = 1;
    static double DEFAULT_TOLER = 0.1;
    /* package protected */ boolean checkAnalyticalDerivatives;
    /* package protected */ 
    /* package protected */
    double theErrorDef = 1;/* package protected */ MultiFunction theFCN;
    /* package protected */ 
    /* package protected */
    int theNumCall;/* package protected */ MnUserParameterState theState;
    /* package protected */ MnStrategy theStrategy;
    /* package protected */ boolean useAnalyticalDerivatives;
    /* package protected */
    MnApplication(MultiFunction fcn, MnUserParameterState state, MnStrategy stra) {
        theFCN = fcn;
        theState = state;
        theStrategy = stra;
        checkAnalyticalDerivatives = true;
        useAnalyticalDerivatives = true;
    }

    MnApplication(MultiFunction fcn, MnUserParameterState state, MnStrategy stra, int nfcn) {
        theFCN = fcn;
        theState = state;
        theStrategy = stra;
        theNumCall = nfcn;
        checkAnalyticalDerivatives = true;
        useAnalyticalDerivatives = true;
    }

    /**
     * <p>MultiFunction.</p>
     *
     * @return a {@link MultiFunction} object.
     */
    public MultiFunction MultiFunction() {
        return theFCN;
    }

    /**
     * add free parameter
     *
     * @param err a double.
     * @param val a double.
     * @param name a {@link java.lang.String} object.
     */
    public void add(String name, double val, double err) {
        theState.add(name, val, err);
    }

    /**
     * add limited parameter
     *
     * @param up a double.
     * @param low a double.
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     * @param err a double.
     */
    public void add(String name, double val, double err, double low, double up) {
        theState.add(name, val, err, low, up);
    }

    /**
     * add const parameter
     *
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     */
    public void add(String name, double val) {
        theState.add(name, val);
    }

    /**
     * <p>checkAnalyticalDerivatives.</p>
     *
     * @return a boolean.
     */
    public boolean checkAnalyticalDerivatives() {
        return checkAnalyticalDerivatives;
    }

    /**
     * <p>covariance.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     */
    public MnUserCovariance covariance() {
        return theState.covariance();
    }

    /**
     * <p>error.</p>
     *
     * @param index a int.
     * @return a double.
     */
    public double error(int index) {
        return theState.error(index);
    }

    /**
     * <p>error.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a double.
     */
    public double error(String name) {
        return theState.error(name);
    }

    /**
     * <p>errorDef.</p>
     *
     * @return a double.
     */
    public double errorDef() {
        return theErrorDef;
    }

    /**
     * <p>errors.</p>
     *
     * @return an array of double.
     */
    public double[] errors() {
        return theState.errors();
    }

    double ext2int(int i, double value) {
        return theState.ext2int(i, value);
    }

    int extOfInt(int i) {
        return theState.extOfInt(i);
    }

    //interaction via external number of parameter

    /**
     * <p>fix.</p>
     *
     * @param index a int.
     */
    public void fix(int index) {
        theState.fix(index);
    }

    //interaction via name of parameter
    /**
     * <p>fix.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void fix(String name) {
        theState.fix(name);
    }

    /**
     * convert name into external number of parameter
     *
     * @param name a {@link java.lang.String} object.
     * @return a int.
     */
    public int index(String name) {
        return theState.index(name);
    }

    // transformation internal <-> external

    double int2ext(int i, double value) {
        return theState.int2ext(i, value);
    }

    int intOfExt(int i) {
        return theState.intOfExt(i);
    }

    /**
     * <p>minimize.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     */
    public FunctionMinimum minimize() {
        return minimize(DEFAULT_MAXFCN);
    }
    /**
     * <p>minimize.</p>
     *
     * @param maxfcn a int.
     * @return a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     */
    public FunctionMinimum minimize(int maxfcn) {
        return minimize(maxfcn, DEFAULT_TOLER);
    }

    /**
     * Causes minimization of the FCN and returns the result in form of a
     * FunctionMinimum.
     *
     * @param maxfcn specifies the (approximate) maximum number of function
     * calls after which the calculation will be stopped even if it has not yet
     * converged.
     * @param toler specifies the required tolerance on the function value at
     * the minimum. The default tolerance value is 0.1, and the minimization
     * will stop when the estimated vertical distance to the minimum (EDM) is
     * less than 0:001*tolerance*errorDef
     * @return a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     */
    public FunctionMinimum minimize(int maxfcn, double toler) {
        if (!theState.isValid()) {
            throw new IllegalStateException("Invalid state");
        }
        int npar = variableParameters();
        if (maxfcn == 0) {
            maxfcn = 200 + 100 * npar + 5 * npar * npar;
        }
        FunctionMinimum min = minimizer().minimize(theFCN, theState, theStrategy, maxfcn, toler, theErrorDef, useAnalyticalDerivatives, checkAnalyticalDerivatives);
        theNumCall += min.nfcn();
        theState = min.userState();
        return min;
    }

    abstract ModularFunctionMinimizer minimizer();

    // facade: forward interface of MnUserParameters and MnUserTransformation
    List<MinuitParameter> minuitParameters() {
        return theState.minuitParameters();
    }

    /**
     * convert external number into name of parameter
     *
     * @param index a int.
     * @return a {@link java.lang.String} object.
     */
    public String name(int index) {
        return theState.name(index);
    }

    /**
     * <p>numOfCalls.</p>
     *
     * @return a int.
     */
    public int numOfCalls() {
        return theNumCall;
    }

    /**
     * access to single parameter
     * @param i
     * @return 
     */
    MinuitParameter parameter(int i) {
        return theState.parameter(i);
    }

    /**
     * <p>parameters.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     */
    public MnUserParameters parameters() {
        return theState.parameters();
    }

    /**
     * access to parameters and errors in column-wise representation
     *
     * @return an array of double.
     */
    public double[] params() {
         return theState.params();
     }

    /**
     * <p>precision.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnMachinePrecision} object.
     */
    public MnMachinePrecision precision() {
        return theState.precision();
    }

    /**
     * <p>release.</p>
     *
     * @param index a int.
     */
    public void release(int index) {
        theState.release(index);
    }

    /**
     * <p>release.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void release(String name) {
        theState.release(name);
    }

    /**
     * <p>removeLimits.</p>
     *
     * @param index a int.
     */
    public void removeLimits(int index) {
        theState.removeLimits(index);
    }

    /**
     * <p>removeLimits.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void removeLimits(String name) {
        theState.removeLimits(name);
    }

    /**
     * Minuit does a check of the user gradient at the beginning, if this is not
     * wanted the set this to "false".
     *
     * @param check a boolean.
     */
    public void setCheckAnalyticalDerivatives(boolean check) {
        checkAnalyticalDerivatives = check;
    }

    /**
     * <p>setError.</p>
     *
     * @param index a int.
     * @param err a double.
     */
    public void setError(int index, double err) {
        theState.setError(index, err);
    }

    /**
     * <p>setError.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param err a double.
     */
    public void setError(String name, double err) {
        theState.setError(name, err);
    }

    /**
     * errorDef() is the error definition of the function. E.g. is 1 if function
     * is Chi2 and 0.5 if function is -logLikelihood. If the user wants instead
     * the 2-sigma errors, errorDef() = 4, as Chi2(x+n*sigma) = Chi2(x) + n*n.
     *
     * @param errorDef a double.
     */
    public void setErrorDef(double errorDef) {
        theErrorDef = errorDef;
    }

    /**
     * <p>setLimits.</p>
     *
     * @param index a int.
     * @param low a double.
     * @param up a double.
     */
    public void setLimits(int index, double low, double up) {
        theState.setLimits(index, low, up);
    }
    /**
     * <p>setLimits.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param low a double.
     * @param up a double.
     */
    public void setLimits(String name, double low, double up) {
        theState.setLimits(name, low, up);
    }

    /**
     * <p>setPrecision.</p>
     *
     * @param prec a double.
     */
    public void setPrecision(double prec) {
        theState.setPrecision(prec);
    }

    /**
     * By default if the function to be minimized implements MultiFunction then
     * the analytical gradient provided by the function will be used. Set this
     * to
     * <CODE>false</CODE> to disable this behaviour and force numerical
     * calculation of the gradient.
     *
     * @param use a boolean.
     */
    public void setUseAnalyticalDerivatives(boolean use) {
        useAnalyticalDerivatives = use;
    }

    /**
     * <p>setValue.</p>
     *
     * @param index a int.
     * @param val a double.
     */
    public void setValue(int index, double val) {
        theState.setValue(index, val);
    }

    /**
     * <p>setValue.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     */
    public void setValue(String name, double val) {
        theState.setValue(name, val);
    }

    /**
     * <p>state.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState state() {
        return theState;
    }

    /**
     * <p>strategy.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnStrategy} object.
     */
    public MnStrategy strategy() {
        return theStrategy;
    }

    /**
     * <p>useAnalyticalDerivaties.</p>
     *
     * @return a boolean.
     */
    public boolean useAnalyticalDerivaties() {
        return useAnalyticalDerivatives;
    }

    /**
     * <p>value.</p>
     *
     * @param index a int.
     * @return a double.
     */
    public double value(int index) {
        return theState.value(index);
    }

    /**
     * <p>value.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a double.
     */
    public double value(String name) {
        return theState.value(name);
    }

    /**
     * <p>variableParameters.</p>
     *
     * @return a int.
     */
    public int variableParameters() {
        return theState.variableParameters();
    }
}
