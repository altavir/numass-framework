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
package hep.dataforge.stat.likelihood;

import hep.dataforge.exceptions.NamingException;
import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.names.NameList;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.stat.parametric.AbstractParametricValue;
import hep.dataforge.stat.parametric.ParametricUtils;
import hep.dataforge.stat.parametric.ParametricValue;
import hep.dataforge.values.Values;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.LoggerFactory;

/**
 * Automatically calculated Likelihood based on fit state
 *
 * @author Alexander Nozik
 */
public class LogLikelihood implements ParametricValue {

    private final FitState source;

    public LogLikelihood(FitState source) {
        this.source = source;
        if (!source.getModel().providesProb()) {
            LoggerFactory.getLogger(getClass())
                    .info("LogLikelihood : Model does not provide definition for point destribution. Using -chi^2/2 for logLikelihood.");
        }
    }

    public double derivValue(String derivParName, ParamSet pars) {
        return source.getLogProbDeriv(derivParName, pars);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double derivValue(String derivParName, Values pars) throws NotDefinedException, NamingException {
        return derivValue(derivParName, new ParamSet(pars));
    }

    @Override
    public NameList getNames() {
        return source.getModel().getNames();
    }

    /**
     * Get a likelihood function calculated as {@code value - offset} to avoid very large or very small values
     *
     * @return
     */
    public ParametricValue getLikelihood(double offset) {
        return new AbstractParametricValue(this) {
            @Override
            public double derivValue(String derivParName, Values pars) throws NotDefinedException, NamingException {
                return value(pars)*LogLikelihood.this.derivValue(derivParName, pars);
            }

            @Override
            public boolean providesDeriv(String name) {
                return LogLikelihood.this.providesDeriv(name);
            }

            @Override
            public double value(Values pars) throws NamingException {
                return FastMath.exp(LogLikelihood.this.value(pars) - offset);
            }
        };
    }

    /**
     * The likelihood function without offset
     * @return
     */
    public ParametricValue getLikelihood() {
        return getLikelihood(0);
    }

    /**
     * Get Likelihood function, rescaling it to be 1 in the given point
     * @param offsetPoint
     * @return
     */
    public ParametricValue getLikelihood(Values offsetPoint) {
        return getLikelihood(value(offsetPoint));
    }

    public UnivariateFunction getLogLikelihoodProjection(final String axisName, final Values allPar) {
        return ParametricUtils.getNamedProjection(this, axisName, allPar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean providesDeriv(String name) {
        return source.getModel().providesProbDeriv(name);
    }

    public double value(ParamSet pars) {
        return source.getLogProb(pars);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double value(Values pars) throws NamingException {
        return value(new ParamSet(pars));
    }

}
