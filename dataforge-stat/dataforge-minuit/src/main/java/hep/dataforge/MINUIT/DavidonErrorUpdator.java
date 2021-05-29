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

import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @version $Id$
 */
class DavidonErrorUpdator implements MinimumErrorUpdator {

        /** {@inheritDoc} */
    @Override
    public MinimumError update(MinimumState s0, MinimumParameters p1, FunctionGradient g1) {
        MnAlgebraicSymMatrix V0 = s0.error().invHessian();
        RealVector dx = MnUtils.sub(p1.vec(), s0.vec());
        RealVector dg = MnUtils.sub(g1.getGradient(), s0.gradient().getGradient());

        double delgam = MnUtils.innerProduct(dx, dg);
        double gvg = MnUtils.similarity(dg, V0);

        RealVector vg = MnUtils.mul(V0, dg);

        MnAlgebraicSymMatrix Vupd = MnUtils.sub(MnUtils.div(MnUtils.outerProduct(dx), delgam), MnUtils.div(MnUtils.outerProduct(vg), gvg));

        if (delgam > gvg) {
            Vupd = MnUtils.add(Vupd, MnUtils.mul(MnUtils.outerProduct(MnUtils.sub(MnUtils.div(dx, delgam), MnUtils.div(vg, gvg))), gvg));
        }

        double sum_upd = MnUtils.absoluteSumOfElements(Vupd);
        Vupd = MnUtils.add(Vupd, V0);

        double dcov = 0.5 * (s0.error().dcovar() + sum_upd / MnUtils.absoluteSumOfElements(Vupd));

        return new MinimumError(Vupd, dcov);
    }
}
