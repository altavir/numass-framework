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
 * MnScan scans the value of the user function by varying one parameter. It is
 * sometimes useful for debugging the user function or finding a reasonable
 * starting point.
 *
 * @version $Id$
 * @author Darksnake
 */
public class MnScan extends MnApplication {

    private ScanMinimizer theMinimizer = new ScanMinimizer();

    /**
     * construct from MultiFunction + double[] for parameters and errors
     * with default strategy
     *
     * @param err an array of double.
     * @param par an array of double.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnScan(MultiFunction fcn, double[] par, double[] err) {
        this(fcn, par, err, DEFAULT_STRATEGY);
    }

    /**
     * construct from MultiFunction + double[] for parameters and errors
     *
     * @param stra a int.
     * @param err an array of double.
     * @param fcn a {@link MultiFunction} object.
     * @param par an array of double.
     */
    public MnScan(MultiFunction fcn, double[] par, double[] err, int stra) {
        this(fcn, new MnUserParameterState(par, err), new MnStrategy(stra));
    }

    /**
     * construct from MultiFunction + double[] for parameters and
     * MnUserCovariance with default strategy
     *
     * @param cov a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     * @param par an array of double.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnScan(MultiFunction fcn, double[] par, MnUserCovariance cov) {
        this(fcn, par, cov, DEFAULT_STRATEGY);
    }

    /**
     * construct from MultiFunction + double[] for parameters and
     * MnUserCovariance
     *
     * @param stra a int.
     * @param cov a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     * @param fcn a {@link MultiFunction} object.
     * @param par an array of double.
     */
    public MnScan(MultiFunction fcn, double[] par, MnUserCovariance cov, int stra) {
        this(fcn, new MnUserParameterState(par, cov), new MnStrategy(stra));
    }

    /**
     * construct from MultiFunction + MnUserParameters with default
     * strategy
     *
     * @param fcn a {@link MultiFunction} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     */
    public MnScan(MultiFunction fcn, MnUserParameters par) {
        this(fcn, par, DEFAULT_STRATEGY);
    }

    /**
     * construct from MultiFunction + MnUserParameters
     *
     * @param stra a int.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnScan(MultiFunction fcn, MnUserParameters par, int stra) {
        this(fcn, new MnUserParameterState(par), new MnStrategy(stra));
    }

    /**
     * construct from MultiFunction + MnUserParameters + MnUserCovariance
     * with default strategy
     *
     * @param cov a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnScan(MultiFunction fcn, MnUserParameters par, MnUserCovariance cov) {
        this(fcn, par, cov, DEFAULT_STRATEGY);
    }

    /**
     * construct from MultiFunction + MnUserParameters + MnUserCovariance
     *
     * @param stra a int.
     * @param cov a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     * @param fcn a {@link MultiFunction} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     */
    public MnScan(MultiFunction fcn, MnUserParameters par, MnUserCovariance cov, int stra) {
        this(fcn, new MnUserParameterState(par, cov), new MnStrategy(stra));
    }

    /**
     * construct from MultiFunction + MnUserParameterState + MnStrategy
     *
     * @param str a {@link hep.dataforge.MINUIT.MnStrategy} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnScan(MultiFunction fcn, MnUserParameterState par, MnStrategy str) {
        super(fcn, par, str);
    }

    @Override
    ModularFunctionMinimizer minimizer() {
        return theMinimizer;
    }

    /**
     * <p>scan.</p>
     *
     * @param par a int.
     * @return a {@link java.util.List} object.
     */
    public List<Range> scan(int par) {
        return scan(par, 41);
    }

    /**
     * <p>scan.</p>
     *
     * @param par a int.
     * @param maxsteps a int.
     * @return a {@link java.util.List} object.
     */
    public List<Range> scan(int par, int maxsteps) {
        return scan(par, maxsteps, 0, 0);
    }

    /**
     * Scans the value of the user function by varying parameter number par,
     * leaving all other parameters fixed at the current value. If par is not
     * specified, all variable parameters are scanned in sequence. The number of
     * points npoints in the scan is 40 by default, and cannot exceed 100. The
     * range of the scan is by default 2 standard deviations on each side of the
     * current best value, but can be specified as from low to high. After each
     * scan, if a new minimum is found, the best parameter values are retained
     * as start values for future scans or minimizations. The curve resulting
     * from each scan can be plotted on the output terminal using MnPlot in
     * order to show the approximate behaviour of the function.
     *
     * @param high a double.
     * @param par a int.
     * @param maxsteps a int.
     * @param low a double.
     * @return a {@link java.util.List} object.
     */
    public List<Range> scan(int par, int maxsteps, double low, double high) {
        MnParameterScan scan = new MnParameterScan(theFCN, theState.parameters());
        double amin = scan.fval();

        List<Range> result = scan.scan(par, maxsteps, low, high);
        if (scan.fval() < amin) {
            theState.setValue(par, scan.parameters().value(par));
            amin = scan.fval();
        }
        return result;
    }
}
