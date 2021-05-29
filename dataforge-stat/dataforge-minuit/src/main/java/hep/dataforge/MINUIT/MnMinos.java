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

/**
 * API class for Minos error analysis (asymmetric errors). Minimization has to
 * be done before and minimum must be valid; possibility to ask only for one
 * side of the Minos error;
 *
 * @version $Id$
 * @author Darksnake
 */
public class MnMinos {
    private MultiFunction theFCN;
    private FunctionMinimum theMinimum;
    private MnStrategy theStrategy;

    /**
     * construct from FCN + minimum
     *
     * @param fcn a {@link MultiFunction} object.
     * @param min a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     */
    public MnMinos(MultiFunction fcn, FunctionMinimum min) {
        this(fcn, min, MnApplication.DEFAULT_STRATEGY);
    }

    /**
     * construct from FCN + minimum + strategy
     *
     * @param stra a int.
     * @param min a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnMinos(MultiFunction fcn, FunctionMinimum min, int stra) {
        this(fcn, min, new MnStrategy(stra));
    }

    /**
     * construct from FCN + minimum + strategy
     *
     * @param stra a {@link hep.dataforge.MINUIT.MnStrategy} object.
     * @param min a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnMinos(MultiFunction fcn, FunctionMinimum min, MnStrategy stra) {
        theFCN = fcn;
        theMinimum = min;
        theStrategy = stra;
    }
    
//    public MnMinos(MultiFunction fcn, MnUserParameterState state, double errDef, MnStrategy stra) {
//        theFCN = fcn;
//        theStrategy = stra;
//        
//        MinimumState minState = null;
//                
//        MnUserTransformation transformation = state.getTransformation();
//        
//        MinimumSeed seed = new MinimumSeed(minState, transformation);
//        
//        theMinimum = new FunctionMinimum(seed,errDef);
//    }
    

    /**
     * <p>loval.</p>
     *
     * @param par a int.
     * @return a {@link hep.dataforge.MINUIT.MnCross} object.
     */
    public MnCross loval(int par) {
        return loval(par, 1);
    }

    /**
     * <p>loval.</p>
     *
     * @param par a int.
     * @param errDef a double.
     * @return a {@link hep.dataforge.MINUIT.MnCross} object.
     */
    public MnCross loval(int par, double errDef) {
        return loval(par, errDef, MnApplication.DEFAULT_MAXFCN);
    }

    /**
     * <p>loval.</p>
     *
     * @param par a int.
     * @param errDef a double.
     * @param maxcalls a int.
     * @return a {@link hep.dataforge.MINUIT.MnCross} object.
     */
    public MnCross loval(int par, double errDef, int maxcalls) {
        errDef *= theMinimum.errorDef();
        assert (theMinimum.isValid());
        assert (!theMinimum.userState().parameter(par).isFixed());
        assert (!theMinimum.userState().parameter(par).isConst());
        if (maxcalls == 0) {
            int nvar = theMinimum.userState().variableParameters();
            maxcalls = 2 * (nvar + 1) * (200 + 100 * nvar + 5 * nvar * nvar);
        }
        int[] para = {par};
        
        MnUserParameterState upar = theMinimum.userState().copy();
        double err = upar.error(par);
        double val = upar.value(par) - err;
        double[] xmid = {val};
        double[] xdir = {-err};
        
        int ind = upar.intOfExt(par);
        MnAlgebraicSymMatrix m = theMinimum.error().matrix();
        double xunit = Math.sqrt(errDef / err);
        for (int i = 0; i < m.nrow(); i++) {
            if (i == ind) {
                continue;
            }
            double xdev = xunit * m.get(ind, i);
            int ext = upar.extOfInt(i);
            upar.setValue(ext, upar.value(ext) - xdev);
        }
        
        upar.fix(par);
        upar.setValue(par, val);
        
        double toler = 0.1;
        MnFunctionCross cross = new MnFunctionCross(theFCN, upar, theMinimum.fval(), theStrategy, errDef);
        
        MnCross aopt = cross.cross(para, xmid, xdir, toler, maxcalls);
        
        if (aopt.atLimit()) {
            MINUITPlugin.logStatic("MnMinos parameter " + par + " is at lower limit.");
        }
        if (aopt.atMaxFcn()) {
            MINUITPlugin.logStatic("MnMinos maximum number of function calls exceeded for parameter " + par);
        }
        if (aopt.newMinimum()) {
            MINUITPlugin.logStatic("MnMinos new minimum found while looking for parameter " + par);
        }
        if (!aopt.isValid()) {
            MINUITPlugin.logStatic("MnMinos could not find lower value for parameter " + par + ".");
        }
        
        return aopt;
        
    }

    /**
     * <p>lower.</p>
     *
     * @param par a int.
     * @return a double.
     */
    public double lower(int par) {
        return lower(par, 1);
    }

    /**
     * <p>lower.</p>
     *
     * @param par a int.
     * @param errDef a double.
     * @return a double.
     */
    public double lower(int par, double errDef) {
        return lower(par, errDef, MnApplication.DEFAULT_MAXFCN);
    }

    /**
     * calculate one side (negative or positive error) of the parameter
     *
     * @param maxcalls a int.
     * @param par a int.
     * @param errDef a double.
     * @return a double.
     */
    public double lower(int par, double errDef, int maxcalls) {
        MnUserParameterState upar = theMinimum.userState();
        double err = theMinimum.userState().error(par);
        MnCross aopt = loval(par, errDef, maxcalls);
        double lower = aopt.isValid() ? -1. * err * (1. + aopt.value()) : (aopt.atLimit() ? upar.parameter(par).lowerLimit() : upar.value(par));
        return lower;
    }

    /**
     * <p>minos.</p>
     *
     * @param par a int.
     * @return a {@link hep.dataforge.MINUIT.MinosError} object.
     */
    public MinosError minos(int par) {
        return minos(par, 1.);
    }

    /**
     * <p>minos.</p>
     *
     * @param par a int.
     * @param errDef a double.
     * @return a {@link hep.dataforge.MINUIT.MinosError} object.
     */
    public MinosError minos(int par, double errDef) {
        return minos(par, errDef, MnApplication.DEFAULT_MAXFCN);
    }

    /**
     * Causes a MINOS error analysis to be performed on the parameter whose
     * number is specified. MINOS errors may be expensive to calculate, but are
     * very reliable since they take account of non-linearities in the problem
     * as well as parameter correlations, and are in general asymmetric.
     *
     * @param maxcalls Specifies the (approximate) maximum number of function
     * calls per parameter requested, after which the calculation will be
     * stopped for that parameter.
     * @param errDef a double.
     * @param par a int.
     * @return a {@link hep.dataforge.MINUIT.MinosError} object.
     */
    public MinosError minos(int par, double errDef, int maxcalls) {
        assert (theMinimum.isValid());
        assert (!theMinimum.userState().parameter(par).isFixed());
        assert (!theMinimum.userState().parameter(par).isConst());

        MnCross up = upval(par, errDef, maxcalls);
        MnCross lo = loval(par, errDef, maxcalls);

        return new MinosError(par, theMinimum.userState().value(par), lo, up);
    }

    /**
     * <p>range.</p>
     *
     * @param par a int.
     * @return
     */
    public Range range(int par) {
        return range(par, 1);
    }

    /**
     * <p>range.</p>
     *
     * @param par a int.
     * @param errDef a double.
     * @return
     */
    public Range range(int par, double errDef) {
        return range(par, errDef, MnApplication.DEFAULT_MAXFCN);
    }

    /**
     * Causes a MINOS error analysis for external parameter n.
     *
     * @param maxcalls a int.
     * @param errDef a double.
     * @return The lower and upper bounds of parameter
     * @param par a int.
     */
    public Range range(int par, double errDef, int maxcalls) {
        MinosError mnerr = minos(par, errDef, maxcalls);
        return mnerr.range();
    }

    /**
     * <p>upper.</p>
     *
     * @param par a int.
     * @return a double.
     */
    public double upper(int par) {
        return upper(par, 1);
    }

    /**
     * <p>upper.</p>
     *
     * @param par a int.
     * @param errDef a double.
     * @return a double.
     */
    public double upper(int par, double errDef) {
        return upper(par, errDef, MnApplication.DEFAULT_MAXFCN);
    }

    /**
     * <p>upper.</p>
     *
     * @param par a int.
     * @param errDef a double.
     * @param maxcalls a int.
     * @return a double.
     */
    public double upper(int par, double errDef, int maxcalls) {
        MnUserParameterState upar = theMinimum.userState();
        double err = theMinimum.userState().error(par);
        MnCross aopt = upval(par, errDef, maxcalls);
        double upper = aopt.isValid() ? err * (1. + aopt.value()) : (aopt.atLimit() ? upar.parameter(par).upperLimit() : upar.value(par));
        return upper;
    }

    /**
     * <p>upval.</p>
     *
     * @param par a int.
     * @return a {@link hep.dataforge.MINUIT.MnCross} object.
     */
    public MnCross upval(int par) {
        return upval(par, 1);
    }

    /**
     * <p>upval.</p>
     *
     * @param par a int.
     * @param errDef a double.
     * @return a {@link hep.dataforge.MINUIT.MnCross} object.
     */
    public MnCross upval(int par, double errDef) {
        return upval(par, errDef, MnApplication.DEFAULT_MAXFCN);
    }

    /**
     * <p>upval.</p>
     *
     * @param par a int.
     * @param errDef a double.
     * @param maxcalls a int.
     * @return a {@link hep.dataforge.MINUIT.MnCross} object.
     */
    public MnCross upval(int par, double errDef, int maxcalls) {
        errDef *= theMinimum.errorDef();
        assert (theMinimum.isValid());
        assert (!theMinimum.userState().parameter(par).isFixed());
        assert (!theMinimum.userState().parameter(par).isConst());
        if (maxcalls == 0) {
            int nvar = theMinimum.userState().variableParameters();
            maxcalls = 2 * (nvar + 1) * (200 + 100 * nvar + 5 * nvar * nvar);
        }

        int[] para = {par};

        MnUserParameterState upar = theMinimum.userState().copy();
        double err = upar.error(par);
        double val = upar.value(par) + err;
        double[] xmid = {val};
        double[] xdir = {err};

        int ind = upar.intOfExt(par);
        MnAlgebraicSymMatrix m = theMinimum.error().matrix();
        double xunit = Math.sqrt(errDef / err);
        for (int i = 0; i < m.nrow(); i++) {
            if (i == ind) {
                continue;
            }
            double xdev = xunit * m.get(ind, i);
            int ext = upar.extOfInt(i);
            upar.setValue(ext, upar.value(ext) + xdev);
        }

        upar.fix(par);
        upar.setValue(par, val);

        double toler = 0.1;
        MnFunctionCross cross = new MnFunctionCross(theFCN, upar, theMinimum.fval(), theStrategy, errDef);
        MnCross aopt = cross.cross(para, xmid, xdir, toler, maxcalls);

        if (aopt.atLimit()) {
            MINUITPlugin.logStatic("MnMinos parameter " + par + " is at upper limit.");
        }
        if (aopt.atMaxFcn()) {
            MINUITPlugin.logStatic("MnMinos maximum number of function calls exceeded for parameter " + par);
        }
        if (aopt.newMinimum()) {
            MINUITPlugin.logStatic("MnMinos new minimum found while looking for parameter " + par);
        }
        if (!aopt.isValid()) {
            MINUITPlugin.logStatic("MnMinos could not find upper value for parameter " + par + ".");
        }

        return aopt;
    }
}
