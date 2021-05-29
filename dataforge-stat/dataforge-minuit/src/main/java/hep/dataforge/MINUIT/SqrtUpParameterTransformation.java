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
class SqrtUpParameterTransformation {
    // derivative of transformation from internal to external

    double dInt2Ext(double value, double upper) {
        return -value / (Math.sqrt(value * value + 1.));
    }

    // transformation from external to internal
    double ext2int(double value, double upper, MnMachinePrecision prec) {
        double yy = upper - value + 1.;
        double yy2 = yy * yy;
        if (yy2 < (1. + prec.eps2())) {
            return 8 * Math.sqrt(prec.eps2());
        } else {
            return Math.sqrt(yy2 - 1);
        }
    }

    // transformation from internal to external
    double int2ext(double value, double upper) {
        return upper + 1. - Math.sqrt(value * value + 1.);
    }
}
