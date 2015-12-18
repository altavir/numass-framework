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
package inr.numass.prop;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.functions.AbstractParametricFunction;
import hep.dataforge.maths.NamedDoubleSet;

/**
 *
 * @author Darksnake
 */
public class SplitNormalSpectrum extends AbstractParametricFunction {

    private static final String[] list = {"amp", "pos", "sigma", "dsigma"};

    public SplitNormalSpectrum() {
        super(list);
    }

    @Override
    public double derivValue(String parName, double x, NamedDoubleSet set) {
        throw new NotDefinedException();
    }

    @Override
    public boolean providesDeriv(String name) {
        return false;
    }

    @Override
    public double value(double x, NamedDoubleSet set) {
        double amp = set.getValue("amp");
        double pos = set.getValue("pos");
        double sigma = set.getValue("sigma");
        double dsigma = set.getValue("dsigma");

        double t;
        if (x <= pos) {
            t = (x - pos) / (sigma - dsigma * sigma);
        } else {
            t = (x - pos) / (sigma + dsigma * sigma);
        }

        return amp / Math.sqrt(2 * Math.PI) / sigma * Math.exp(-t * t / 2);
    }

}
