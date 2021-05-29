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

import hep.dataforge.stat.fit.MINUITPlugin;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @version $Id$
 */
class VariableMetricBuilder implements MinimumBuilder {
    private DavidonErrorUpdator theErrorUpdator;
    private VariableMetricEDMEstimator theEstimator;

    VariableMetricBuilder() {
        theEstimator = new VariableMetricEDMEstimator();
        theErrorUpdator = new DavidonErrorUpdator();
    }

    DavidonErrorUpdator errorUpdator() {
        return theErrorUpdator;
    }

    VariableMetricEDMEstimator estimator() {
        return theEstimator;
    }

    /** {@inheritDoc} */
    @Override
    public FunctionMinimum minimum(MnFcn fcn, GradientCalculator gc, MinimumSeed seed, MnStrategy strategy, int maxfcn, double edmval) {
        FunctionMinimum min = minimum(fcn, gc, seed, maxfcn, edmval);
        if ((strategy.strategy() == 2) || (strategy.strategy() == 1 && min.error().dcovar() > 0.05)) {
            MinimumState st = new MnHesse(strategy).calculate(fcn, min.state(), min.seed().trafo(), 0);
            min.add(st);
        }
        if (!min.isValid()) {
            MINUITPlugin.logStatic("FunctionMinimum is invalid.");
        }

        return min;
    }

    FunctionMinimum minimum(MnFcn fcn, GradientCalculator gc, MinimumSeed seed, int maxfcn, double edmval) {
        edmval *= 0.0001;

        if (seed.parameters().vec().getDimension() == 0) {
            return new FunctionMinimum(seed, fcn.errorDef());
        }

        MnMachinePrecision prec = seed.precision();

        List<MinimumState> result = new ArrayList<>(8);

        double edm = seed.state().edm();

        if (edm < 0.) {
            MINUITPlugin.logStatic("VariableMetricBuilder: initial matrix not pos.def.");
            if (seed.error().isPosDef()) {
                throw new RuntimeException("Something is wrong!");
            }
            return new FunctionMinimum(seed, fcn.errorDef());
        }

        result.add(seed.state());

        // iterate until edm is small enough or max # of iterations reached
        edm *= (1. + 3. * seed.error().dcovar());
        RealVector step;// = new ArrayRealVector(seed.gradient().getGradient().getDimension());
        do {
            MinimumState s0 = result.get(result.size() - 1);

            step = MnUtils.mul(MnUtils.mul(s0.error().invHessian(), s0.gradient().getGradient()), -1);

            double gdel = MnUtils.innerProduct(step, s0.gradient().getGradient());
            if (gdel > 0.) {
                MINUITPlugin.logStatic("VariableMetricBuilder: matrix not pos.def.");
                MINUITPlugin.logStatic("gdel > 0: " + gdel);
                s0 = MnPosDef.test(s0, prec);
                step = MnUtils.mul(MnUtils.mul(s0.error().invHessian(), s0.gradient().getGradient()), -1);
                gdel = MnUtils.innerProduct(step, s0.gradient().getGradient());
                MINUITPlugin.logStatic("gdel: " + gdel);
                if (gdel > 0.) {
                    result.add(s0);
                    return new FunctionMinimum(seed, result, fcn.errorDef());
                }
            }
            MnParabolaPoint pp = MnLineSearch.search(fcn, s0.parameters(), step, gdel, prec);
            if (Math.abs(pp.y() - s0.fval()) < prec.eps()) {
                MINUITPlugin.logStatic("VariableMetricBuilder: no improvement");
                break; //no improvement
            }
            MinimumParameters p = new MinimumParameters(MnUtils.add(s0.vec(), MnUtils.mul(step, pp.x())), pp.y());
            FunctionGradient g = gc.gradient(p, s0.gradient());

            edm = estimator().estimate(g, s0.error());
            if (edm < 0.) {
                MINUITPlugin.logStatic("VariableMetricBuilder: matrix not pos.def.");
                MINUITPlugin.logStatic("edm < 0");
                s0 = MnPosDef.test(s0, prec);
                edm = estimator().estimate(g, s0.error());
                if (edm < 0.) {
                    result.add(s0);
                    return new FunctionMinimum(seed, result, fcn.errorDef());
                }
            }
            MinimumError e = errorUpdator().update(s0, p, g);
            result.add(new MinimumState(p, e, g, edm, fcn.numOfCalls()));
            //     result[0] = MinimumState(p, e, g, edm, fcn.numOfCalls());
            edm *= (1. + 3. * e.dcovar());
        } while (edm > edmval && fcn.numOfCalls() < maxfcn);

        if (fcn.numOfCalls() >= maxfcn) {
            MINUITPlugin.logStatic("VariableMetricBuilder: call limit exceeded.");
            return new FunctionMinimum(seed, result, fcn.errorDef(), new FunctionMinimum.MnReachedCallLimit());
        }

        if (edm > edmval) {
            if (edm < Math.abs(prec.eps2() * result.get(result.size() - 1).fval())) {
                MINUITPlugin.logStatic("VariableMetricBuilder: machine accuracy limits further improvement.");
                return new FunctionMinimum(seed, result, fcn.errorDef());
            } else if (edm < 10. * edmval) {
                return new FunctionMinimum(seed, result, fcn.errorDef());
            } else {
                MINUITPlugin.logStatic("VariableMetricBuilder: finishes without convergence.");
                MINUITPlugin.logStatic("VariableMetricBuilder: edm= " + edm + " requested: " + edmval);
                return new FunctionMinimum(seed, result, fcn.errorDef(), new FunctionMinimum.MnAboveMaxEdm());
            }
        }

        return new FunctionMinimum(seed, result, fcn.errorDef());
    }
}
