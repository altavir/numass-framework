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


import hep.dataforge.functions.AbstractParametricFunction;
import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.maths.NamedDoubleSet;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.names.AbstractNamedSet;
import static hep.dataforge.names.NamedUtils.combineNamesWithEquals;
import inr.numass.NumassContext;
import inr.numass.models.RangedNamedSetSpectrum;
import inr.numass.models.Transmission;
import static java.lang.Double.isNaN;
import java.util.Arrays;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 *
 * @author Darksnake
 */
public abstract class AbstractTransmission extends AbstractNamedSet implements Transmission {

    protected final UnivariateIntegrator integrator = NumassContext.defaultIntegrator;
    //    private final UnivariateIntegrator integrator = new SimpsonIntegrator(1e-2, Double.MAX_VALUE, 3, 64);

    public AbstractTransmission(String[] list) {
        super(list);
    }

    @Override
    public ParametricFunction getConvolutedSpectrum(final RangedNamedSetSpectrum bare) {
        return new AbstractParametricFunction(combineNamesWithEquals(this.namesAsArray(), bare.namesAsArray())) {
            //  int maxEval = GlobalContext.getPreferences().getInt("INTEGR_POINTS", 1000);
            @Override
            public double derivValue(String parName, double out, NamedDoubleSet set) {
                return integrate(getDerivProduct(parName, bare, set, out), getBorders(set, out));
            }

            @Override
            public boolean providesDeriv(String name) {
                return this.names().contains(name) || bare.providesDeriv(name);
            }

            @Override
            public double value(double out, NamedDoubleSet set) {
                return integrate(getProduct(bare, set, out), getBorders(set, out));
            }
        };
    }

    /**
     * Provides borders for integration. the number of borders must be >= 2
     * @param pars
     * @param out
     * @return 
     */
    protected abstract double[] getBorders(NamedDoubleSet pars, double out);

    /**
     * Интегрирует функцию по кускам. Переданный масив должен содержать не
     * меньше двух значений
     *
     * @param func
     * @param borders
     * @return
     */
    protected double integrate(UnivariateFunction func, double[] borders) {
        if (borders.length < 2) {
            throw new IllegalArgumentException();
        }

        double res = 0;

        Arrays.sort(borders);

        for (int i = 0; i < borders.length - 1; i++) {
            if (borders[i + 1] > borders[i]) {
                res += integrator.integrate(func, borders[i], borders[i + 1]);
            }
        }

        return res;
    }

    UnivariateFunction getDerivProduct(final String name, final ParametricFunction bare, final NamedDoubleSet pars, final double out) {
        return (double in) -> {
            double res1;
            double res2;
            res1 = bare.derivValue(name, in, pars) * getValue(pars, in, out);
            res2 = bare.value(in, pars) * getDeriv(name, pars, in, out);
            return res1 + res2;
        };
    }

    UnivariateFunction getProduct(final ParametricFunction bare, final NamedDoubleSet pars, final double out) {
        return (double in) -> {
            double res = bare.value(in, pars) * getValue(pars, in, out);
            assert !isNaN(res);
            return res;
        };
    }

    public ParametricFunction getResponseFunction(final double in) {
        return new AbstractParametricFunction(AbstractTransmission.this.names()) {

            @Override
            public double derivValue(String parName, double out, NamedDoubleSet set) {
                return AbstractTransmission.this.getDeriv(parName, set, in, out);
            }

            @Override
            public boolean providesDeriv(String name) {
                return AbstractTransmission.this.providesDeriv(name);
            }

            @Override
            public double value(double out, NamedDoubleSet set) {
                return AbstractTransmission.this.getValue(set, in, out);
            }
        };
    }

    /**
     * Функция отклика с дополнительным параметром. Никаких производных по этому
     * параметру, разумеется нет.
     *
     * @param positionParameterName
     * @return
     */
    public ParametricFunction getResponseFunction(String positionParameterName) {
        return new AbstractParametricFunction(AbstractTransmission.this.names()) {

            @Override
            public double derivValue(String parName, double out, NamedDoubleSet set) {
                return AbstractTransmission.this.getDeriv(parName, set, set.getValue(positionParameterName), out);
            }

            @Override
            public boolean providesDeriv(String name) {
                return AbstractTransmission.this.providesDeriv(name) && !name.equals(positionParameterName);
            }

            @Override
            public double value(double out, NamedDoubleSet set) {
                return AbstractTransmission.this.getValue(set, set.getValue(positionParameterName), out);
            }
        };
    }

}
