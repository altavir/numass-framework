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
package inr.numass.models;

import hep.dataforge.functions.AbstractParametricFunction;
import hep.dataforge.functions.ParametricFunction;
import static hep.dataforge.names.NamedUtils.combineNamesWithEquals;
import hep.dataforge.utils.MultiCounter;
import hep.dataforge.values.NamedValueSet;
import hep.dataforge.values.ValueProvider;

/**
 *
 * @author Darksnake
 */
public class NBkgSpectrum extends AbstractParametricFunction {

    private static final String[] list = {"N", "bkg"};

    public MultiCounter counter = new MultiCounter(this.getClass().getName());
    ParametricFunction source;

    public NBkgSpectrum(ParametricFunction source) {
        super(combineNamesWithEquals(source.namesAsArray(), list));
        this.source = source;
    }

    @Override
    public double derivValue(String parName, double x, NamedValueSet set) {
        this.counter.increase(parName);
        switch (parName) {
            case "N":
                return source.value(x, set);
            case "bkg":
                return 1;
            default:
                return getN(set) * source.derivValue(parName, x, set);
        }
    }

    private double getBkg(ValueProvider set) {
        return set.getDouble("bkg");
    }

    private double getN(ValueProvider set) {
        return set.getDouble("N");
    }

    @Override
    public boolean providesDeriv(String name) {
        switch (name) {
            case "N":
                return true;
            case "bkg":
                return true;
            default:
                return this.source.providesDeriv(name);
        }
    }

    @Override
    public double value(double x, NamedValueSet set) {
        this.counter.increase("value");
        return getN(set) * source.value(x, set) + getBkg(set);
    }

    @Override
    protected double getDefaultValue(String name) {
        switch (name) {
            case "bkg":
                return 0;
            case "N":
                return 1;
            default:
                return super.getDefaultValue(name);
        }
    }

}
