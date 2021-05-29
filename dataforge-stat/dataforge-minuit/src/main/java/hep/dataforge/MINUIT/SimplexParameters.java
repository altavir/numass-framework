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

import java.util.List;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @version $Id$
 */
class SimplexParameters {
    private int theJHigh;
    private int theJLow;
    private List<Pair<Double, RealVector>> theSimplexParameters;

    SimplexParameters(List<Pair<Double, RealVector>> simpl, int jh, int jl) {
        theSimplexParameters = simpl;
        theJHigh = jh;
        theJLow = jl;
    }

    ArrayRealVector dirin() {
        ArrayRealVector dirin = new ArrayRealVector(theSimplexParameters.size() - 1);
        for (int i = 0; i < theSimplexParameters.size() - 1; i++) {
            double pbig = theSimplexParameters.get(0).getSecond().getEntry(i);
            double plit = pbig;
            for (Pair<Double, RealVector> theSimplexParameter : theSimplexParameters) {
                if (theSimplexParameter.getSecond().getEntry(i) < plit) {
                    plit = theSimplexParameter.getSecond().getEntry(i);
                }
                if (theSimplexParameter.getSecond().getEntry(i) > pbig) {
                    pbig = theSimplexParameter.getSecond().getEntry(i);
                }
            }
            dirin.setEntry(i, pbig - plit);
        }

        return dirin;
    }

    double edm() {
        return theSimplexParameters.get(jh()).getFirst() - theSimplexParameters.get(jl()).getFirst();
    }

    Pair<Double, RealVector> get(int i) {
        return theSimplexParameters.get(i);
    }

    int jh() {
        return theJHigh;
    }

    int jl() {
        return theJLow;
    }

    List<Pair<Double, RealVector>> simplex() {
        return theSimplexParameters;
    }

    void update(double y, RealVector p) {
        theSimplexParameters.set(jh(), new Pair<>(y, p));
        if (y < theSimplexParameters.get(jl()).getFirst()) {
            theJLow = jh();
        }
        
        int jh = 0;
        for (int i = 1; i < theSimplexParameters.size(); i++) {
            if (theSimplexParameters.get(i).getFirst() > theSimplexParameters.get(jh).getFirst()) {
                jh = i;
            }
        }
        theJHigh = jh;
    }
}
