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
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @version $Id$
 */
class MnPosDef {

    static MinimumState test(MinimumState st, MnMachinePrecision prec) {
        MinimumError err = test(st.error(), prec);
        return new MinimumState(st.parameters(), err, st.gradient(), st.edm(), st.nfcn());

    }

    static MinimumError test(MinimumError e, MnMachinePrecision prec) {
        MnAlgebraicSymMatrix err = e.invHessian().copy();
        if (err.size() == 1 && err.get(0, 0) < prec.eps()) {
            err.set(0, 0, 1.);
            return new MinimumError(err, new MinimumError.MnMadePosDef());
        }
        if (err.size() == 1 && err.get(0, 0) > prec.eps()) {
            return e;
        }
        //   std::cout<<"MnPosDef init matrix= "<<err<<std::endl;

        double epspdf = Math.max(1.e-6, prec.eps2());
        double dgmin = err.get(0, 0);

        for (int i = 0; i < err.nrow(); i++) {
            if (err.get(i, i) < prec.eps2()) {
                MINUITPlugin.logStatic("negative or zero diagonal element " + i + " in covariance matrix");
            }
            if (err.get(i, i) < dgmin) {
                dgmin = err.get(i, i);
            }
        }
        double dg = 0.;
        if (dgmin < prec.eps2()) {
            dg = 1. + epspdf - dgmin;
            //     dg = 0.5*(1. + epspdf - dgmin);
            MINUITPlugin.logStatic("added " + dg + " to diagonal of error matrix");
        }

        RealVector s = new ArrayRealVector(err.nrow());
        MnAlgebraicSymMatrix p = new MnAlgebraicSymMatrix(err.nrow());
        for (int i = 0; i < err.nrow(); i++) {
            err.set(i, i, err.get(i, i) + dg);
            if (err.get(i, i) < 0.) {
                err.set(i, i, 1.);
            }
            s.setEntry(i, 1. / Math.sqrt(err.get(i, i)));
            for (int j = 0; j <= i; j++) {
                p.set(i, j, err.get(i, j) * s.getEntry(i) * s.getEntry(j));
            }
        }

        //   std::cout<<"MnPosDef p: "<<p<<std::endl;
        RealVector eval = p.eigenvalues();
        double pmin = eval.getEntry(0);
        double pmax = eval.getEntry(eval.getDimension() - 1);
        //   std::cout<<"pmin= "<<pmin<<" pmax= "<<pmax<<std::endl;
        pmax = Math.max(Math.abs(pmax), 1.);
        if (pmin > epspdf * pmax) {
            return e;
        }

        double padd = 0.001 * pmax - pmin;
        MINUITPlugin.logStatic("eigenvalues: ");
        for (int i = 0; i < err.nrow(); i++) {
            err.set(i, i, err.get(i, i) * (1. + padd));
            MINUITPlugin.logStatic(Double.toString(eval.getEntry(i)));
        }
        //   std::cout<<"MnPosDef final matrix: "<<err<<std::endl;
        MINUITPlugin.logStatic("matrix forced pos-def by adding " + padd + " to diagonal");
        //   std::cout<<"eigenvalues: "<<eval<<std::endl;
        return new MinimumError(err, new MinimumError.MnMadePosDef());

    }
}
