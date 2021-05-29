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
package hep.dataforge.stat.fit;

import hep.dataforge.MINUIT.FunctionMinimum;
import hep.dataforge.MINUIT.MnPrint;
import hep.dataforge.MINUIT.MnUserParameters;
import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.maths.functions.MultiFunction;

import java.io.PrintWriter;

class MINUITUtils {

    static MultiFunction getFcn(FitState source, ParamSet allPar, String[] fitPars) {
        return new MnFunc(source, allPar, fitPars);
    }

    static MnUserParameters getFitParameters(ParamSet set, String[] fitPars) {
        MnUserParameters pars = new MnUserParameters();
        int i;
        Param par;

        for (i = 0; i < fitPars.length; i++) {
            par = set.getByName(fitPars[i]);
            pars.add(fitPars[i], par.getValue(), par.getErr());

            if ((par.getLowerBound() > Double.NEGATIVE_INFINITY) && (par.getUpperBound() < Double.POSITIVE_INFINITY)) {
                pars.setLimits(i, par.getLowerBound(), par.getUpperBound());
            } else if (par.getLowerBound() > Double.NEGATIVE_INFINITY) {
                pars.setLowerLimit(i, par.getLowerBound());
            } else if (par.getUpperBound() < Double.POSITIVE_INFINITY) {
                pars.setUpperLimit(i, par.getUpperBound());
            }

//            pars.release(i);
        }
        return pars;
    }

    static ParamSet getValueSet(ParamSet allPar, String[] names, double[] values) {
        assert values.length == names.length;
        assert allPar.getNames().contains(names);

        ParamSet vector = allPar.copy();

        for (int i = 0; i < values.length; i++) {
            vector.setParValue(names[i], values[i]);

        }
        return vector;
    }

    static boolean isValidArray(double[] ar) {
        for (int i = 0; i < ar.length; i++) {
            if (Double.isNaN(ar[i])) {
                return false;
            }

        }
        return true;
    }

    /**
     * <p>
     * printMINUITResult.</p>
     *
     * @param out a {@link java.io.PrintWriter} object.
     * @param minimum a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     */
    public static void printMINUITResult(PrintWriter out, FunctionMinimum minimum) {
        out.println();
        out.println("***MINUIT INTERNAL FIT INFORMATION***");
        out.println();
        MnPrint.print(out, minimum);
        out.println();
        out.println("***END OF MINUIT INTERNAL FIT INFORMATION***");
        out.println();

    }

    static class MnFunc implements MultiFunction {

        FitState source;
        ParamSet allPar;
        String[] fitPars;

        public MnFunc(FitState source, ParamSet allPar, String[] fitPars) {
            this.source = source;
            this.allPar = allPar;
            this.fitPars = fitPars;
            assert source.getModel().getNames().contains(fitPars);
        }

        @Override
        public double value(double[] doubles) {
            assert MINUITUtils.isValidArray(doubles);
            assert doubles.length == fitPars.length;

            return -2 * source.getLogProb(getValueSet(allPar, fitPars, doubles));
//                    source.getChi2(getValueSet(allPar, fitPars, doubles));
        }

        @Override
        public double derivValue(int n, double[] doubles) throws NotDefinedException {
            assert MINUITUtils.isValidArray(doubles);
            assert doubles.length == this.getDimension();

            ParamSet set = getValueSet(allPar, fitPars, doubles);

//            double res;
//            double d, s, deriv;
//
//            res = 0;
//            for (int i = 0; i < source.getDataNum(); i++) {
//                d = source.getDis(i, set);
//                s = source.getDispersion(i, set);
//                if (source.modelProvidesDerivs(fitPars[n])) {
//                    deriv = source.getDisDeriv(fitPars[n], i, set);
//                } else {
//                    throw new NotDefinedException();
//                    // Такого не должно быть, поскольку мы где-то наверху должы были проверить, что производные все есть.
//                }
//                res += 2 * d * deriv / s;
//            }
            return -2 * source.getLogProbDeriv(fitPars[n], set);
        }

        @Override
        public int getDimension() {
            return fitPars.length;
        }

        @Override
        public boolean providesDeriv(int n) {
            return source.modelProvidesDerivs(fitPars[n]);
        }
    }
}
