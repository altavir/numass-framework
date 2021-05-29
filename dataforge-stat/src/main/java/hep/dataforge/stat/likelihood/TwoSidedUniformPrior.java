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
package hep.dataforge.stat.likelihood;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.names.NameList;
import hep.dataforge.stat.parametric.ParametricValue;
import hep.dataforge.values.Values;

/**
 * <p>TwoSidedUniformPrior class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class TwoSidedUniformPrior implements ParametricValue {

    private final String parName;
    private final NameList names;
    private double lowerBorder;
    private double norm;
    private double upperBorder;

    /**
     * <p>Constructor for TwoSidedUniformPrior.</p>
     *
     * @param parName a {@link java.lang.String} object.
     * @param lowerBorder a double.
     * @param upperBorder a double.
     */
    public TwoSidedUniformPrior(String parName, double lowerBorder, double upperBorder) {
        this.names = new NameList(parName);
        this.parName = parName;
        if (upperBorder <= lowerBorder) {
            throw new IllegalArgumentException("Wrong interval borders.");
        }
        this.lowerBorder = lowerBorder;
        this.upperBorder = upperBorder;
        this.norm = 1 / (upperBorder - lowerBorder);
    }

    /** {@inheritDoc} */
    @Override
    public double derivValue(String derivParName, Values pars) throws NotDefinedException {
        if (!this.parName.equals(derivParName)) {
            return 0;
        }
        double parValue = pars.getDouble(parName);
        if (parValue <= this.lowerBorder) {
            return Double.POSITIVE_INFINITY;
        } else if (parValue >= this.upperBorder) {
            return Double.NEGATIVE_INFINITY;
        } else {
            return 0;
        }

    }

    /** {@inheritDoc} */
    @Override
    public NameList getNames() {
        return names;
    }

    /** {@inheritDoc} */
    @Override
    public String[] namesAsArray() {
        String[] list = new String[1];
        list[0] = this.parName;
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public boolean providesDeriv(String name) {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * Нормированная априорная информация
     */
    @Override
    public double value(Values pars) {
        double parValue = pars.getDouble(parName);
        if (parValue >= lowerBorder && parValue <= upperBorder) {
            return norm;
        } else {
            return 0;
        }
    }
}
