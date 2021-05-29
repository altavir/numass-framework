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
import org.apache.commons.math3.linear.RealVector;


/**
 * Функция, которая помнит количество вызовов себя и ErrorDef
 * @version $Id$
 */
class MnFcn {
    private double theErrorDef;

    private MultiFunction theFCN;
    protected int theNumCall;

    MnFcn(MultiFunction fcn, double errorDef) {
        theFCN = fcn;
        theNumCall = 0;
        theErrorDef = errorDef;
    }

    double errorDef() {
        return theErrorDef;
    }

    MultiFunction fcn() {
        return theFCN;
    }

    int numOfCalls() {
        return theNumCall;
    }

    double value(RealVector v) {
        theNumCall++;
        return theFCN.value(v.toArray());
    }
}
