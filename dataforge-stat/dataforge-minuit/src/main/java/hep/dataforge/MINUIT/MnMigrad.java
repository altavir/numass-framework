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

/**
 * MnMigrad provides minimization of the function by the method of MIGRAD, the
 * most efficient and complete single method, recommended for general functions,
 * and the functionality for parameters interaction. It also retains the result
 * from the last minimization in case the user may want to do subsequent
 * minimization steps with parameter interactions in between the minimization
 * requests. The minimization produces as a by-product the error matrix of the
 * parameters, which is usually reliable unless warning messages are produced.
 *
 * @version $Id$
 * @author Darksnake
 */
public class MnMigrad extends MnApplication {
    private VariableMetricMinimizer theMinimizer = new VariableMetricMinimizer();

    /**
     * construct from MultiFunction + double[] for parameters and errors
     * with default strategy
     *
     * @param err an array of double.
     * @param par an array of double.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnMigrad(MultiFunction fcn, double[] par, double[] err) {
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
    public MnMigrad(MultiFunction fcn, double[] par, double[] err, int stra) {
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
    public MnMigrad(MultiFunction fcn, double[] par, MnUserCovariance cov) {
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
    public MnMigrad(MultiFunction fcn, double[] par, MnUserCovariance cov, int stra) {
        this(fcn, new MnUserParameterState(par, cov), new MnStrategy(stra));
    }

    /**
     * construct from MultiFunction + MnUserParameters with default
     * strategy
     *
     * @param fcn a {@link MultiFunction} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     */
    public MnMigrad(MultiFunction fcn, MnUserParameters par) {
        this(fcn, par, DEFAULT_STRATEGY);
    }

    /**
     * construct from MultiFunction + MnUserParameters
     *
     * @param stra a int.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnMigrad(MultiFunction fcn, MnUserParameters par, int stra) {
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
    public MnMigrad(MultiFunction fcn, MnUserParameters par, MnUserCovariance cov) {
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
    public MnMigrad(MultiFunction fcn, MnUserParameters par, MnUserCovariance cov, int stra) {
        this(fcn, new MnUserParameterState(par, cov), new MnStrategy(stra));
    }

    /**
     * construct from MultiFunction + MnUserParameterState + MnStrategy
     *
     * @param str a {@link hep.dataforge.MINUIT.MnStrategy} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnMigrad(MultiFunction fcn, MnUserParameterState par, MnStrategy str) {
        super(fcn, par, str);
    }

    @Override
    ModularFunctionMinimizer minimizer() {
        return theMinimizer;
    }
}
