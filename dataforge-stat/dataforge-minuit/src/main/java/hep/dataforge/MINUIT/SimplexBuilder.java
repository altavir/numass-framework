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
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @version $Id$
 */
class SimplexBuilder implements MinimumBuilder {

        /** {@inheritDoc} */
    @Override
    public FunctionMinimum minimum(MnFcn mfcn, GradientCalculator gc, MinimumSeed seed, MnStrategy strategy, int maxfcn, double minedm) {
        MnMachinePrecision prec = seed.precision();
        RealVector x = seed.parameters().vec().copy();
        RealVector step = MnUtils.mul(seed.gradient().getStep(), 10.);

        int n = x.getDimension();
        double wg = 1. / n;
        double alpha = 1., beta = 0.5, gamma = 2., rhomin = 4., rhomax = 8.;
        double rho1 = 1. + alpha;
        double rho2 = 1. + alpha * gamma;

        List<Pair<Double, RealVector>> simpl = new ArrayList<>(n + 1);
        simpl.add(new Pair<>(seed.fval(), x.copy()));

        int jl = 0, jh = 0;
        double amin = seed.fval(), aming = seed.fval();

        for (int i = 0; i < n; i++) {
            double dmin = 8. * prec.eps2() * (Math.abs(x.getEntry(i)) + prec.eps2());
            if (step.getEntry(i) < dmin) {
                step.setEntry(i, dmin);
            }
            x.setEntry(i, x.getEntry(i) + step.getEntry(i));
            double tmp = mfcn.value(x);
            if (tmp < amin) {
                amin = tmp;
                jl = i + 1;
            }
            if (tmp > aming) {
                aming = tmp;
                jh = i + 1;
            }
            simpl.add(new Pair<>(tmp, x.copy()));
            x.setEntry(i, x.getEntry(i) - step.getEntry(i));
        }
        SimplexParameters simplex = new SimplexParameters(simpl, jh, jl);

        do {
            amin = simplex.get(jl).getFirst();
            jl = simplex.jl();
            jh = simplex.jh();
            RealVector pbar = new ArrayRealVector(n);
            for (int i = 0; i < n + 1; i++) {
                if (i == jh) {
                    continue;
                }
                pbar = MnUtils.add(pbar, MnUtils.mul(simplex.get(i).getSecond(), wg));
            }

            RealVector pstar = MnUtils.sub(MnUtils.mul(pbar, 1. + alpha), MnUtils.mul(simplex.get(jh).getSecond(), alpha));
            double ystar = mfcn.value(pstar);

            if (ystar > amin) {
                if (ystar < simplex.get(jh).getFirst()) {
                    simplex.update(ystar, pstar);
                    if (jh != simplex.jh()) {
                        continue;
                    }
                }
                RealVector pstst = MnUtils.add(MnUtils.mul(simplex.get(jh).getSecond(), beta), MnUtils.mul(pbar, 1. - beta));
                double ystst = mfcn.value(pstst);
                if (ystst > simplex.get(jh).getFirst()) {
                    break;
                }
                simplex.update(ystst, pstst);
                continue;
            }

            RealVector pstst = MnUtils.add(MnUtils.mul(pstar, gamma), MnUtils.mul(pbar, 1. - gamma));
            double ystst = mfcn.value(pstst);

            double y1 = (ystar - simplex.get(jh).getFirst()) * rho2;
            double y2 = (ystst - simplex.get(jh).getFirst()) * rho1;
            double rho = 0.5 * (rho2 * y1 - rho1 * y2) / (y1 - y2);
            if (rho < rhomin) {
                if (ystst < simplex.get(jl).getFirst()) {
                    simplex.update(ystst, pstst);
                } else {
                    simplex.update(ystar, pstar);
                }
                continue;
            }
            if (rho > rhomax) {
                rho = rhomax;
            }
            RealVector prho = MnUtils.add(MnUtils.mul(pbar, rho), MnUtils.mul(simplex.get(jh).getSecond(), 1. - rho));
            double yrho = mfcn.value(prho);
            if (yrho < simplex.get(jl).getFirst() && yrho < ystst) {
                simplex.update(yrho, prho);
                continue;
            }
            if (ystst < simplex.get(jl).getFirst()) {
                simplex.update(ystst, pstst);
                continue;
            }
            if (yrho > simplex.get(jl).getFirst()) {
                if (ystst < simplex.get(jl).getFirst()) {
                    simplex.update(ystst, pstst);
                } else {
                    simplex.update(ystar, pstar);
                }
                continue;
            }
            if (ystar > simplex.get(jh).getFirst()) {
                pstst = MnUtils.add(MnUtils.mul(simplex.get(jh).getSecond(), beta), MnUtils.mul(pbar, 1 - beta));
                ystst = mfcn.value(pstst);
                if (ystst > simplex.get(jh).getFirst()) {
                    break;
                }
                simplex.update(ystst, pstst);
            }
        } while (simplex.edm() > minedm && mfcn.numOfCalls() < maxfcn);

        amin = simplex.get(jl).getFirst();
        jl = simplex.jl();
        jh = simplex.jh();

        RealVector pbar = new ArrayRealVector(n);
        for (int i = 0; i < n + 1; i++) {
            if (i == jh) {
                continue;
            }
            pbar = MnUtils.add(pbar, MnUtils.mul(simplex.get(i).getSecond(), wg));
        }
        double ybar = mfcn.value(pbar);
        if (ybar < amin) {
            simplex.update(ybar, pbar);
        } else {
            pbar = simplex.get(jl).getSecond();
            ybar = simplex.get(jl).getFirst();
        }

        RealVector dirin = simplex.dirin();
        //   scale to sigmas on parameters werr^2 = dirin^2 * (up/edm)
        dirin = MnUtils.mul(dirin, Math.sqrt(mfcn.errorDef() / simplex.edm()));

        MinimumState st = new MinimumState(new MinimumParameters(pbar, dirin, ybar), simplex.edm(), mfcn.numOfCalls());
        List<MinimumState> states = new ArrayList<>(1);
        states.add(st);

        if (mfcn.numOfCalls() > maxfcn) {
            MINUITPlugin.logStatic("Simplex did not converge, #fcn calls exhausted.");
            return new FunctionMinimum(seed, states, mfcn.errorDef(), new FunctionMinimum.MnReachedCallLimit());
        }
        if (simplex.edm() > minedm) {
            MINUITPlugin.logStatic("Simplex did not converge, edm > minedm.");
            return new FunctionMinimum(seed, states, mfcn.errorDef(), new FunctionMinimum.MnAboveMaxEdm());
        }

        return new FunctionMinimum(seed, states, mfcn.errorDef());
    }
}
