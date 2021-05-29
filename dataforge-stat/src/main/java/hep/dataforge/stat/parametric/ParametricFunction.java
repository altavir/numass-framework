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
package hep.dataforge.stat.parametric;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.names.NameList;
import hep.dataforge.names.NameSetContainer;
import hep.dataforge.values.Values;

/**
 * <p>
 * NamedSpectrum interface.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public interface ParametricFunction extends NameSetContainer {

    double derivValue(String parName, double x, Values set);

    double value(double x, Values set);

    boolean providesDeriv(String name);

    default ParametricFunction derivative(String parName) {
        if (providesDeriv(parName)) {
            return new ParametricFunction() {
                @Override
                public double derivValue(String parName, double x, Values set) {
                    if (getNames().contains(parName)) {
                        throw new NotDefinedException();
                    } else {
                        return 0;
                    }
                }

                @Override
                public double value(double x, Values set) {
                    return ParametricFunction.this.derivValue(parName, x, set);
                }

                @Override
                public boolean providesDeriv(String name) {
                    return !getNames().contains(name);
                }

                @Override
                public NameList getNames() {
                    return ParametricFunction.this.getNames();
                }
            };
        } else {
            throw new NotDefinedException();
        }
    }
}
