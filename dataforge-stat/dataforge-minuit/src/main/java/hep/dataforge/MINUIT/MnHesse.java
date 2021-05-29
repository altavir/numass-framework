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
import hep.dataforge.stat.fit.MINUITPlugin;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;


/**
 * With MnHesse the user can instructs MINUITPlugin to calculate, by finite
 differences, the Hessian or error matrix. That is, it calculates the full
 * matrix of second derivatives of the function with respect to the currently
 * variable parameters, and inverts it.
 *
 * @version $Id$
 * @author Darksnake
 */
public class MnHesse {

    private MnStrategy theStrategy;

    /**
     * default constructor with default strategy
     */
    public MnHesse() {
        theStrategy = new MnStrategy(1);
    }

    /**
     * constructor with user-defined strategy level
     *
     * @param stra a int.
     */
    public MnHesse(int stra) {
        theStrategy = new MnStrategy(stra);
    }

    /**
     * conctructor with specific strategy
     *
     * @param stra a {@link hep.dataforge.MINUIT.MnStrategy} object.
     */
    public MnHesse(MnStrategy stra) {
        theStrategy = stra;
    }

    ///
    /// low-level API
    ///
    /**
     * <p>calculate.</p>
     *
     * @param fcn a {@link MultiFunction} object.
     * @param par an array of double.
     * @param err an array of double.
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState calculate(MultiFunction fcn, double[] par, double[] err) {
        return calculate(fcn, par, err, 0);
    }

    /**
     * FCN + parameters + errors
     *
     * @param maxcalls a int.
     * @param fcn a {@link MultiFunction} object.
     * @param par an array of double.
     * @param err an array of double.
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState calculate(MultiFunction fcn, double[] par, double[] err, int maxcalls) {
        return calculate(fcn, new MnUserParameterState(par, err), maxcalls);
    }

    /**
     * <p>calculate.</p>
     *
     * @param fcn a {@link MultiFunction} object.
     * @param par an array of double.
     * @param cov a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState calculate(MultiFunction fcn, double[] par, MnUserCovariance cov) {
        return calculate(fcn, par, cov, 0);
    }

    /**
     * FCN + parameters + MnUserCovariance
     *
     * @param maxcalls a int.
     * @param fcn a {@link MultiFunction} object.
     * @param par an array of double.
     * @param cov a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState calculate(MultiFunction fcn, double[] par, MnUserCovariance cov, int maxcalls) {
        return calculate(fcn, new MnUserParameterState(par, cov), maxcalls);
    }
    ///
    /// high-level API
    ///

    /**
     * <p>calculate.</p>
     *
     * @param fcn a {@link MultiFunction} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState calculate(MultiFunction fcn, MnUserParameters par) {
        return calculate(fcn, par, 0);
    }

    /**
     * FCN + MnUserParameters
     *
     * @param maxcalls a int.
     * @param fcn a {@link MultiFunction} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState calculate(MultiFunction fcn, MnUserParameters par, int maxcalls) {
        return calculate(fcn, new MnUserParameterState(par), maxcalls);
    }

    /**
     * <p>calculate.</p>
     *
     * @param fcn a {@link MultiFunction} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     * @param cov a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState calculate(MultiFunction fcn, MnUserParameters par, MnUserCovariance cov) {
        return calculate(fcn, par, 0);
    }

    /**
     * FCN + MnUserParameters + MnUserCovariance
     *
     * @param maxcalls a int.
     * @param fcn a {@link MultiFunction} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     * @param cov a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState calculate(MultiFunction fcn, MnUserParameters par, MnUserCovariance cov, int maxcalls) {
        return calculate(fcn, new MnUserParameterState(par, cov), maxcalls);
    }

    /**
     * FCN + MnUserParameterState
     *
     * @param maxcalls a int.
     * @param fcn a {@link MultiFunction} object.
     * @param state a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState calculate(MultiFunction fcn, MnUserParameterState state, int maxcalls) {
        double errDef = 1; // FixMe!
        int n = state.variableParameters();
        MnUserFcn mfcn = new MnUserFcn(fcn, errDef, state.getTransformation());
        RealVector x = new ArrayRealVector(n);
        for (int i = 0; i < n; i++) {
            x.setEntry(i, state.intParameters().get(i));
        }
        double amin = mfcn.value(x);
        Numerical2PGradientCalculator gc = new Numerical2PGradientCalculator(mfcn, state.getTransformation(), theStrategy);
        MinimumParameters par = new MinimumParameters(x, amin);
        FunctionGradient gra = gc.gradient(par);
        MinimumState tmp = calculate(mfcn, new MinimumState(par, new MinimumError(new MnAlgebraicSymMatrix(n), 1.), gra, state.edm(), state.nfcn()), state.getTransformation(), maxcalls);

        return new MnUserParameterState(tmp, errDef, state.getTransformation());
    }
    ///
    /// internal interface
    ///

    MinimumState calculate(MnFcn mfcn, MinimumState st, MnUserTransformation trafo, int maxcalls) {
        MnMachinePrecision prec = trafo.precision();
        // make sure starting at the right place
        double amin = mfcn.value(st.vec());
        double aimsag = Math.sqrt(prec.eps2()) * (Math.abs(amin) + mfcn.errorDef());

        // diagonal elements first

        int n = st.parameters().vec().getDimension();
        if (maxcalls == 0) {
            maxcalls = 200 + 100 * n + 5 * n * n;
        }

        MnAlgebraicSymMatrix vhmat = new MnAlgebraicSymMatrix(n);
        RealVector g2 = st.gradient().getGradientDerivative().copy();
        RealVector gst = st.gradient().getStep().copy();
        RealVector grd = st.gradient().getGradient().copy();
        RealVector dirin = st.gradient().getStep().copy();
        RealVector yy = new ArrayRealVector(n);
        if (st.gradient().isAnalytical()) {
            InitialGradientCalculator igc = new InitialGradientCalculator(mfcn, trafo, theStrategy);
            FunctionGradient tmp = igc.gradient(st.parameters());
            gst = tmp.getStep().copy();
            dirin = tmp.getStep().copy();
            g2 = tmp.getGradientDerivative().copy();
        }
        try {
            RealVector x = st.parameters().vec().copy();

            for (int i = 0; i < n; i++) {

                double xtf = x.getEntry(i);
                double dmin = 8. * prec.eps2() * (Math.abs(xtf) + prec.eps2());
                double d = Math.abs(gst.getEntry(i));
                if (d < dmin) {
                    d = dmin;
                }

                for (int icyc = 0; icyc < ncycles(); icyc++) {
                    double sag = 0.;
                    double fs1 = 0.;
                    double fs2 = 0.;
                    int multpy = 0;
                    for (; multpy < 5; multpy++) {
                        x.setEntry(i, xtf + d);
                        fs1 = mfcn.value(x);
                        x.setEntry(i, xtf - d);
                        fs2 = mfcn.value(x);
                        x.setEntry(i, xtf);
                        sag = 0.5 * (fs1 + fs2 - 2. * amin);
                        if (sag > prec.eps2()) {
                            break;
                        }
                        if (trafo.parameter(i).hasLimits()) {
                            if (d > 0.5) {
                                throw new MnHesseFailedException("MnHesse: 2nd derivative zero for parameter");
                            }
                            d *= 10.;
                            if (d > 0.5) {
                                d = 0.51;
                            }
                            continue;
                        }
                        d *= 10.;
                    }
                    if (multpy >= 5) {
                        throw new MnHesseFailedException("MnHesse: 2nd derivative zero for parameter");
                    }

                    double g2bfor = g2.getEntry(i);
                    g2.setEntry(i, 2. * sag / (d * d));
                    grd.setEntry(i, (fs1 - fs2) / (2. * d));
                    gst.setEntry(i, d);
                    dirin.setEntry(i, d);
                    yy.setEntry(i, fs1);
                    double dlast = d;
                    d = Math.sqrt(2. * aimsag / Math.abs(g2.getEntry(i)));
                    if (trafo.parameter(i).hasLimits()) {
                        d = Math.min(0.5, d);
                    }
                    if (d < dmin) {
                        d = dmin;
                    }

                    // see if converged
                    if (Math.abs((d - dlast) / d) < tolerstp()) {
                        break;
                    }
                    if (Math.abs((g2.getEntry(i) - g2bfor) / g2.getEntry(i)) < tolerg2()) {
                        break;
                    }
                    d = Math.min(d, 10. * dlast);
                    d = Math.max(d, 0.1 * dlast);
                }
                vhmat.set(i, i, g2.getEntry(i));
                if (mfcn.numOfCalls() - st.nfcn() > maxcalls) {
                    throw new MnHesseFailedException("MnHesse: maximum number of allowed function calls exhausted.");
                }
            }

            if (theStrategy.strategy() > 0) {
                // refine first derivative
                HessianGradientCalculator hgc = new HessianGradientCalculator(mfcn, trafo, theStrategy);
                FunctionGradient gr = hgc.gradient(st.parameters(), new FunctionGradient(grd, g2, gst));
                grd = gr.getGradient();
            }

            //off-diagonal elements
            for (int i = 0; i < n; i++) {
                x.setEntry(i, x.getEntry(i) + dirin.getEntry(i));
                for (int j = i + 1; j < n; j++) {
                    x.setEntry(j, x.getEntry(j) + dirin.getEntry(j));
                    double fs1 = mfcn.value(x);
                    double elem = (fs1 + amin - yy.getEntry(i) - yy.getEntry(j)) / (dirin.getEntry(i) * dirin.getEntry(j));
                    vhmat.set(i, j, elem);
                    x.setEntry(j, x.getEntry(j) - dirin.getEntry(j));
                }
                x.setEntry(i, x.getEntry(i) - dirin.getEntry(i));
            }

            //verify if matrix pos-def (still 2nd derivative)
            MinimumError tmp = MnPosDef.test(new MinimumError(vhmat, 1.), prec);
            vhmat = tmp.invHessian();
            try {
                vhmat.invert();
            } catch (SingularMatrixException xx) {
                throw new MnHesseFailedException("MnHesse: matrix inversion fails!");
            }

            FunctionGradient gr = new FunctionGradient(grd, g2, gst);

            if (tmp.isMadePosDef()) {
                MINUITPlugin.logStatic("MnHesse: matrix is invalid!");
                MINUITPlugin.logStatic("MnHesse: matrix is not pos. def.!");
                MINUITPlugin.logStatic("MnHesse: matrix was forced pos. def.");
                return new MinimumState(st.parameters(), new MinimumError(vhmat, new MinimumError.MnMadePosDef()), gr, st.edm(), mfcn.numOfCalls());
            }

            //calculate edm
            MinimumError err = new MinimumError(vhmat, 0.);
            double edm = new VariableMetricEDMEstimator().estimate(gr, err);

            return new MinimumState(st.parameters(), err, gr, edm, mfcn.numOfCalls());
        } catch (MnHesseFailedException x) {
            MINUITPlugin.logStatic(x.getMessage());
            MINUITPlugin.logStatic("MnHesse fails and will return diagonal matrix ");

            for (int j = 0; j < n; j++) {
                double tmp = g2.getEntry(j) < prec.eps2() ? 1. : 1. / g2.getEntry(j);
                vhmat.set(j, j, tmp < prec.eps2() ? 1. : tmp);
            }

            return new MinimumState(st.parameters(), new MinimumError(vhmat, new MinimumError.MnHesseFailed()), st.gradient(), st.edm(), st.nfcn() + mfcn.numOfCalls());

        }
    }

    /// forward interface of MnStrategy
    int ncycles() {
        return theStrategy.hessianNCycles();
    }

    double tolerg2() {
        return theStrategy.hessianG2Tolerance();
    }

    double tolerstp() {
        return theStrategy.hessianStepTolerance();
    }

    private class MnHesseFailedException extends Exception {

        MnHesseFailedException(String message) {
            super(message);
        }
    }
}
