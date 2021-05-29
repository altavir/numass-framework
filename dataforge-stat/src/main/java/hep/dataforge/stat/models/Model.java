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
package hep.dataforge.stat.models;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.meta.Metoid;
import hep.dataforge.names.NameSetContainer;
import hep.dataforge.stat.fit.Param;
import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.stat.parametric.ParametricValue;
import hep.dataforge.tables.ValuesSource;
import hep.dataforge.values.Values;

/**
 *
 * @author Alexander Nozik
 */
public interface Model extends NameSetContainer, Metoid {

    /**
     * Fit function value minus data point value
     *
     * @param point
     * @param pars
     * @return
     */
    double distance(Values point, Values pars);

    /**
     * The derivative of distance
     * @param parName
     * @param point
     * @param pars
     * @return
     * @throws NotDefinedException
     */
    double disDeriv(String parName, Values point, Values pars) throws NotDefinedException;

    /**
     * Inverted weight of the point
     *
     * @param point
     * @param pars
     * @return
     */
    double dispersion(Values point, Values pars);

    /**
     * Provides a ln of probability of obtaining the data point with given
     * parameter set
     *
     * @param point
     * @param pars
     * @return
     */
    double getLogProb(Values point, Values pars) throws NotDefinedException;

    /**
     *
     * @param parName
     * @param point
     * @param pars
     * @return
     * @throws NotDefinedException
     */
    double getLogProbDeriv(String parName, Values point, Values pars) throws NotDefinedException;


    /**
     * The model provides its own definition of point distribution
     *
     * @return
     */
    boolean providesProb();

    /**
     * Model provides point distribution derivative
     *
     * @param name
     * @return
     */
    boolean providesProbDeriv(String name);

    /**
     * Model provicer derivative of distance to data point
     * @param name
     * @return
     */
    boolean providesDeriv(String name);

    /**
     *
     * @param point
     * @return
     */
    ParametricValue getDistanceFunction(Values point);

    /**
     *
     * @param point
     * @return
     */
    ParametricValue getLogProbFunction(Values point);

    /**
     * Пытается угадать набор параметров по набору данных. По-умолчанию этот
     * метод не имеет реализации, но может быть
     *
     * @param data
     * @return
     */
    default ParamSet getParametersGuess(ValuesSource data) {
        throw new NotDefinedException("Initial guess not defined");
    }

    /**
     * Возвращает значение параметра по-умолчанию
     *
     * @param name
     * @return
     */
    default Param getDefaultParameter(String name) {
        throw new NotDefinedException("Default parameter not found");
    }

}
