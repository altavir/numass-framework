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

import java.util.ArrayList;
import java.util.List;

/**
 * The class MnUserParameterState contains the MnUserParameters and the
 * MnUserCovariance. It can be created on input by the user, or by MINUIT itself
 * as user representable format of the result of the minimization.
 *
 * @version $Id$
 * @author Darksnake
 */
public final class MnUserParameterState {

    private MnUserCovariance theCovariance;
    private boolean theCovarianceValid;
    private double theEDM;
    private double theFVal;
    private boolean theGCCValid;
    private MnGlobalCorrelationCoeff theGlobalCC;
    private MnUserCovariance theIntCovariance;
    private List<Double> theIntParameters;
    private int theNFcn;
    private MnUserParameters theParameters;
    private boolean theValid;

    MnUserParameterState() {
        theValid = false;
        theCovarianceValid = false;
        theParameters = new MnUserParameters();
        theCovariance = new MnUserCovariance();
        theIntParameters = new ArrayList<>();
        theIntCovariance = new MnUserCovariance();
    }

    private MnUserParameterState(MnUserParameterState other) {
        theValid = other.theValid;
        theCovarianceValid = other.theCovarianceValid;
        theGCCValid = other.theGCCValid;

        theFVal = other.theFVal;
        theEDM = other.theEDM;
        theNFcn = other.theNFcn;

        theParameters = other.theParameters.copy();
        theCovariance = other.theCovariance;
        theGlobalCC = other.theGlobalCC;

        theIntParameters = new ArrayList<>(other.theIntParameters);
        theIntCovariance = other.theIntCovariance.copy();
    }

    /**
     * construct from user parameters (before minimization)
     * @param par
     * @param err
     */
    MnUserParameterState(double[] par, double[] err) {
        theValid = true;
        theParameters = new MnUserParameters(par, err);
        theCovariance = new MnUserCovariance();
        theGlobalCC = new MnGlobalCorrelationCoeff();
        theIntParameters = new ArrayList<>(par.length);
        for (int i = 0; i < par.length; i++) {
            theIntParameters.add(par[i]);
        }
        theIntCovariance = new MnUserCovariance();
    }

    MnUserParameterState(MnUserParameters par) {
        theValid = true;
        theParameters = par;
        theCovariance = new MnUserCovariance();
        theGlobalCC = new MnGlobalCorrelationCoeff();
        theIntParameters = new ArrayList<>(par.variableParameters());
        theIntCovariance = new MnUserCovariance();

        int i = 0;
        for (MinuitParameter ipar : par.parameters()) {
            if (ipar.isConst() || ipar.isFixed()) {
                continue;
            }
            if (ipar.hasLimits()) {
                theIntParameters.add(ext2int(ipar.number(), ipar.value()));
            } else {
                theIntParameters.add(ipar.value());
            }
        }
    }

    /**
     * construct from user parameters + covariance (before minimization)
     * @param nrow
     * @param cov
     */
    MnUserParameterState(double[] par, double[] cov, int nrow) {
        theValid = true;
        theCovarianceValid = true;
        theCovariance = new MnUserCovariance(cov, nrow);
        theGlobalCC = new MnGlobalCorrelationCoeff();
        theIntParameters = new ArrayList<>(par.length);
        theIntCovariance = new MnUserCovariance(cov, nrow);

        double[] err = new double[par.length];
        for (int i = 0; i < par.length; i++) {
            assert (theCovariance.get(i, i) > 0.);
            err[i] = Math.sqrt(theCovariance.get(i, i));
            theIntParameters.add(par[i]);
        }
        theParameters = new MnUserParameters(par, err);
        assert (theCovariance.nrow() == variableParameters());
    }

    MnUserParameterState(double[] par, MnUserCovariance cov) {
        theValid = true;
        theCovarianceValid = true;
        theCovariance = cov;
        theGlobalCC = new MnGlobalCorrelationCoeff();
        theIntParameters = new ArrayList<>(par.length);
        theIntCovariance = cov.copy();

        if (theCovariance.nrow() != variableParameters()) {
            throw new IllegalArgumentException("Bad covariance size");
        }
        double[] err = new double[par.length];
        for (int i = 0; i < par.length; i++) {
            if (theCovariance.get(i, i) <= 0.) {
                throw new IllegalArgumentException("Bad covariance");
            }
            err[i] = Math.sqrt(theCovariance.get(i, i));
            theIntParameters.add(par[i]);
        }
        theParameters = new MnUserParameters(par, err);
    }

    MnUserParameterState(MnUserParameters par, MnUserCovariance cov) {
        theValid = true;
        theCovarianceValid = true;
        theParameters = par;
        theCovariance = cov;
        theGlobalCC = new MnGlobalCorrelationCoeff();
        theIntParameters = new ArrayList<>();
        theIntCovariance = cov.copy();

        theIntCovariance.scale(0.5);
        int i = 0;
        for (MinuitParameter ipar : par.parameters()) {
            if (ipar.isConst() || ipar.isFixed()) {
                continue;
            }
            if (ipar.hasLimits()) {
                theIntParameters.add(ext2int(ipar.number(), ipar.value()));
            } else {
                theIntParameters.add(ipar.value());
            }
        }
        assert (theCovariance.nrow() == variableParameters());
    }

    /**
     * construct from internal parameters (after minimization)
     * @param trafo
     * @param up
     */
    MnUserParameterState(MinimumState st, double up, MnUserTransformation trafo) {
        theValid = st.isValid();
        theCovarianceValid = false;
        theGCCValid = false;
        theFVal = st.fval();
        theEDM = st.edm();
        theNFcn = st.nfcn();
        theParameters = new MnUserParameters();
        theCovariance = new MnUserCovariance();
        theGlobalCC = new MnGlobalCorrelationCoeff();
        theIntParameters = new ArrayList<>();
        theIntCovariance = new MnUserCovariance();

        for (MinuitParameter ipar : trafo.parameters()) {
            if (ipar.isConst()) {
                add(ipar.name(), ipar.value());
            } else if (ipar.isFixed()) {
                add(ipar.name(), ipar.value(), ipar.error());
                if (ipar.hasLimits()) {
                    if (ipar.hasLowerLimit() && ipar.hasUpperLimit()) {
                        setLimits(ipar.name(), ipar.lowerLimit(), ipar.upperLimit());
                    } else if (ipar.hasLowerLimit() && !ipar.hasUpperLimit()) {
                        setLowerLimit(ipar.name(), ipar.lowerLimit());
                    } else {
                        setUpperLimit(ipar.name(), ipar.upperLimit());
                    }
                }
                fix(ipar.name());
            } else if (ipar.hasLimits()) {
                int i = trafo.intOfExt(ipar.number());
                double err = st.hasCovariance() ? Math.sqrt(2. * up * st.error().invHessian().get(i, i)) : st.parameters().dirin().getEntry(i);
                add(ipar.name(), trafo.int2ext(i, st.vec().getEntry(i)), trafo.int2extError(i, st.vec().getEntry(i), err));
                if (ipar.hasLowerLimit() && ipar.hasUpperLimit()) {
                    setLimits(ipar.name(), ipar.lowerLimit(), ipar.upperLimit());
                } else if (ipar.hasLowerLimit() && !ipar.hasUpperLimit()) {
                    setLowerLimit(ipar.name(), ipar.lowerLimit());
                } else {
                    setUpperLimit(ipar.name(), ipar.upperLimit());
                }
            } else {
                int i = trafo.intOfExt(ipar.number());
                double err = st.hasCovariance() ? Math.sqrt(2. * up * st.error().invHessian().get(i, i)) : st.parameters().dirin().getEntry(i);
                add(ipar.name(), st.vec().getEntry(i), err);
            }
        }

        theCovarianceValid = st.error().isValid();

        if (theCovarianceValid) {
            theCovariance = trafo.int2extCovariance(st.vec(), st.error().invHessian());
            theIntCovariance = new MnUserCovariance(st.error().invHessian().data().clone(), st.error().invHessian().nrow());
            theCovariance.scale(2. * up);
            theGlobalCC = new MnGlobalCorrelationCoeff(st.error().invHessian());
            theGCCValid = true;

            assert (theCovariance.nrow() == variableParameters());
        }

    }

    /**
     * add free parameter name, value, error
     *
     * @param err a double.
     * @param val a double.
     * @param name a {@link java.lang.String} object.
     */
    public void add(String name, double val, double err) {
        theParameters.add(name, val, err);
        theIntParameters.add(val);
        theCovarianceValid = false;
        theGCCValid = false;
        theValid = true;
    }

    /**
     * add limited parameter name, value, lower bound, upper bound
     *
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     * @param low a double.
     * @param err a double.
     * @param up a double.
     */
    public void add(String name, double val, double err, double low, double up) {
         theParameters.add(name, val, err, low, up);
         theCovarianceValid = false;
         theIntParameters.add(ext2int(index(name), val));
         theGCCValid = false;
         theValid = true;
     }

    /**
     * add const parameter name, value
     *
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     */
    public void add(String name, double val) {
        theParameters.add(name, val);
        theValid = true;
    }

    /**
     * <p>copy.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    protected MnUserParameterState copy() {
        return new MnUserParameterState(this);
    }

    /**
     * Covariance matrix in the external representation
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     */
    public MnUserCovariance covariance() {
        return theCovariance;
    }

    /**
     * Returns the expected vertival distance to the minimum (EDM)
     *
     * @return a double.
     */
    public double edm() {
        return theEDM;
    }

    /**
     * <p>error.</p>
     *
     * @param index a int.
     * @return a double.
     */
    public double error(int index) {
        return theParameters.error(index);
    }

    /**
     * <p>error.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a double.
     */
    public double error(String name) {
        return error(index(name));
    }

    /**
     * <p>errors.</p>
     *
     * @return an array of double.
     */
    public double[] errors() {
        return theParameters.errors();
    }

    double ext2int(int i, double val) {
        return theParameters.trafo().ext2int(i, val);
    }

    /**
     * <p>extOfInt.</p>
     *
     * @param internal a int.
     * @return a int.
     */
    public int extOfInt(int internal) {
        return theParameters.trafo().extOfInt(internal);
    }

    /// interaction via external number of parameter

    /**
     * <p>fix.</p>
     *
     * @param e a int.
     */
    public void fix(int e) {
        int i = intOfExt(e);
        if (theCovarianceValid) {
            theCovariance = MnCovarianceSqueeze.squeeze(theCovariance, i);
            theIntCovariance = MnCovarianceSqueeze.squeeze(theIntCovariance, i);
        }
        theIntParameters.remove(i);
        theParameters.fix(e);
        theGCCValid = false;
    }

    /// interaction via name of parameter

    /**
     * <p>fix.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void fix(String name) {
        fix(index(name));
    }

    /**
     * returns the function value at the minimum
     *
     * @return a double.
     */
    public double fval() {
        return theFVal;
    }

    /**
     * transformation internal <-> external
     * @return 
     */
    MnUserTransformation getTransformation() {
        return theParameters.trafo();
    }

    MnGlobalCorrelationCoeff globalCC() {
        return theGlobalCC;
    }

    /**
     * Returns
     * <CODE>true</CODE> if the the state has a valid covariance,
     * <CODE>false</CODE> otherwise.
     *
     * @return a boolean.
     */
    public boolean hasCovariance() {
        return theCovarianceValid;
    }

    /**
     * <p>hasGlobalCC.</p>
     *
     * @return a boolean.
     */
    public boolean hasGlobalCC() {
        return theGCCValid;
    }

    /**
     * convert name into external number of parameter
     *
     * @param name a {@link java.lang.String} object.
     * @return a int.
     */
    public int index(String name) {
        return theParameters.index(name);
    }

    // transformation internal <-> external

    double int2ext(int i, double val) {
        return theParameters.trafo().int2ext(i, val);
    }
    MnUserCovariance intCovariance() {
        return theIntCovariance;
    }

    int intOfExt(int ext) {
        return theParameters.trafo().intOfExt(ext);
    }

    /**
     * Minuit internal representation
     * @return 
     */
    List<Double> intParameters() {
        return theIntParameters;
    }

    /**
     * Returns
     * <CODE>true</CODE> if the the state is valid,
     * <CODE>false</CODE> if not
     *
     * @return a boolean.
     */
    public boolean isValid() {
        return theValid;
    }

    // facade: forward interface of MnUserParameters and MnUserTransformation
    List<MinuitParameter> minuitParameters() {
        return theParameters.parameters();
    }

    /**
     * convert external number into name of parameter
     *
     * @param index a int.
     * @return a {@link java.lang.String} object.
     */
    public String name(int index) {
        return theParameters.name(index);
    }

    /**
     * Returns the number of function calls during the minimization.
     *
     * @return a int.
     */
    public int nfcn() {
        return theNFcn;
    }

    MinuitParameter parameter(int i) {
        return theParameters.parameter(i);
    }

    //user external representation
    MnUserParameters parameters() {
        return theParameters;
    }

    /**
     * access to parameters and errors in column-wise representation
     *
     * @return an array of double.
     */
    public double[] params() {
        return theParameters.params();
    }
    /**
     * <p>precision.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnMachinePrecision} object.
     */
    public MnMachinePrecision precision() {
        return theParameters.precision();
    }

    /**
     * <p>release.</p>
     *
     * @param e a int.
     */
    public void release(int e) {
        theParameters.release(e);
        theCovarianceValid = false;
        theGCCValid = false;
        int i = intOfExt(e);
        if (parameter(e).hasLimits()) {
            theIntParameters.add(i, ext2int(e, parameter(e).value()));
        } else {
            theIntParameters.add(i, parameter(e).value());
        }
    }

    /**
     * <p>release.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void release(String name) {
        release(index(name));
    }

    /**
     * <p>removeLimits.</p>
     *
     * @param e a int.
     */
    public void removeLimits(int e) {
        theParameters.removeLimits(e);
        theCovarianceValid = false;
        theGCCValid = false;
        if (!parameter(e).isFixed() && !parameter(e).isConst()) {
            theIntParameters.set(intOfExt(e), value(e));
        }
    }

    /**
     * <p>removeLimits.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void removeLimits(String name) {
        removeLimits(index(name));
    }

    /**
     * <p>setError.</p>
     *
     * @param e a int.
     * @param err a double.
     * @param err a double.
     */
    public void setError(int e, double err) {
        theParameters.setError(e, err);
    }

    /**
     * <p>setError.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param err a double.
     */
    public void setError(String name, double err) {
        setError(index(name), err);
    }

    /**
     * <p>setLimits.</p>
     *
     * @param e a int.
     * @param low a double.
     * @param up a double.
     */
    public void setLimits(int e, double low, double up) {
        theParameters.setLimits(e, low, up);
        theCovarianceValid = false;
        theGCCValid = false;
        if (!parameter(e).isFixed() && !parameter(e).isConst()) {
            int i = intOfExt(e);
            if (low < theIntParameters.get(i) && theIntParameters.get(i) < up) {
                theIntParameters.set(i, ext2int(e, theIntParameters.get(i)));
            } else {
                theIntParameters.set(i, ext2int(e, 0.5 * (low + up)));
            }
        }
    }

    /**
     * <p>setLimits.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param low a double.
     * @param up a double.
     */
    public void setLimits(String name, double low, double up) {
        setLimits(index(name), low, up);
    }

    /**
     * <p>setLowerLimit.</p>
     *
     * @param e a int.
     * @param low a double.
     */
    public void setLowerLimit(int e, double low) {
        theParameters.setLowerLimit(e, low);
        theCovarianceValid = false;
        theGCCValid = false;
        if (!parameter(e).isFixed() && !parameter(e).isConst()) {
            int i = intOfExt(e);
            if (low < theIntParameters.get(i)) {
                theIntParameters.set(i, ext2int(e, theIntParameters.get(i)));
            } else {
                theIntParameters.set(i, ext2int(e, low + 0.5 * Math.abs(low + 1.)));
            }
        }
    }

    /**
     * <p>setLowerLimit.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param low a double.
     */
    public void setLowerLimit(String name, double low) {
        setLowerLimit(index(name), low);
    }

    /**
     * <p>setPrecision.</p>
     *
     * @param eps a double.
     */
    public void setPrecision(double eps) {
        theParameters.setPrecision(eps);
    }
    /**
     * <p>setUpperLimit.</p>
     *
     * @param e a int.
     * @param up a double.
     */
    public void setUpperLimit(int e, double up) {
        theParameters.setUpperLimit(e, up);
        theCovarianceValid = false;
        theGCCValid = false;
        if (!parameter(e).isFixed() && !parameter(e).isConst()) {
            int i = intOfExt(e);
            if (theIntParameters.get(i) < up) {
                theIntParameters.set(i, ext2int(e, theIntParameters.get(i)));
            } else {
                theIntParameters.set(i, ext2int(e, up - 0.5 * Math.abs(up + 1.)));
            }
        }
    }

    /**
     * <p>setUpperLimit.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param up a double.
     */
    public void setUpperLimit(String name, double up) {
        setUpperLimit(index(name), up);
    }

    /**
     * <p>setValue.</p>
     *
     * @param e a int.
     * @param val a double.
     */
    public void setValue(int e, double val) {
        theParameters.setValue(e, val);
        if (!parameter(e).isFixed() && !parameter(e).isConst()) {
            int i = intOfExt(e);
            if (parameter(e).hasLimits()) {
                theIntParameters.set(i, ext2int(e, val));
            } else {
                theIntParameters.set(i, val);
            }
        }
    }

    /**
     * <p>setValue.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param val a double.
     */
    public void setValue(String name, double val) {
        setValue(index(name), val);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MnPrint.toString(this);
    }

        /**
         * <p>value.</p>
         *
         * @param index a int.
         * @return a double.
         */
    public double value(int index) {
        return theParameters.value(index);
    }

        /**
         * <p>value.</p>
         *
         * @param name a {@link java.lang.String} object.
         * @return a double.
         */
    public double value(String name) {
        return value(index(name));
    }

        /**
         * <p>variableParameters.</p>
         *
         * @return a int.
         */
    public int variableParameters() {
        return theParameters.variableParameters();
    }
}
