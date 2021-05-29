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
import hep.dataforge.names.NameSetContainer;
import hep.dataforge.values.Values;

/**
 * A function mapping parameter set to real value
 *
 * @author Alexander Nozik
 */
public interface ParametricValue extends NameSetContainer {

    /**
     * Value
     * @param pars
     * @return
     */
    double value(Values pars);

    /**
     * Partial derivative value for given parameter
     * @param derivParName
     * @param pars
     * @return
     */
    default double derivValue(String derivParName, Values pars){
        throw new NotDefinedException();
    }

    /**
     * Returns true if this object provides explicit analytical value derivative for given parameter
     *
     * @param name a {@link java.lang.String} object.
     * @return a boolean.
     */
    default boolean providesDeriv(String name){
        return false;
    }
}
