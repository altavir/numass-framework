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

/**
 *
 * @version $Id$
 */
class SinParameterTransformation {

    double dInt2Ext(double value, double upper, double lower) {
        return 0.5 * Math.abs((upper - lower) * Math.cos(value));
    }

    double ext2int(double value, double upper, double lower, MnMachinePrecision prec) {
        double piby2 = 2. * Math.atan(1.);
        double distnn = 8. * Math.sqrt(prec.eps2());
        double vlimhi = piby2 - distnn;
        double vlimlo = -piby2 + distnn;

        double yy = 2. * (value - lower) / (upper - lower) - 1.;
        double yy2 = yy * yy;
        if (yy2 > (1. - prec.eps2())) {
            if (yy < 0.) {
                return vlimlo;
            } else {
                return vlimhi;
            }

        } else {
            return Math.asin(yy);
        }
    }

    double int2ext(double value, double upper, double lower) {
        return lower + 0.5 * (upper - lower) * (Math.sin(value) + 1.);
    }
}
