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

import hep.dataforge.maths.NamedDoubleSet;
import org.apache.commons.math3.special.Erf;

/**
 * Нормированаая функция подложки разрешающей функции пропорционального счетчика
 * @author Darksnake
 */
public class BaseFunction extends AbstractTransmission{

    public BaseFunction() {
        super(new String[0]);
    }

    @Override
    protected double[] getBorders(NamedDoubleSet pars, double out) {
        double[] res = new double[2];
        res[0] = 0;
        res[1] = out;
        return res;
    }

    @Override
    public double getValue(NamedDoubleSet set, double input, double output) {
//        if(output >0 && input>0 && input > output){
//            return 1/input;
//        } else {
//            return 0;
//        }
        double w = set.getValue("w")*Math.sqrt(input);
        return 1/input * Erf.erfc((output-input)/w)/2d;
    }

    @Override
    public double getDeriv(String name, NamedDoubleSet set, double input, double output) {
        return 0;
    }

    @Override
    public boolean providesDeriv(String name) {
        return true;
    }
    

    
}
