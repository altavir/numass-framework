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

import inr.numass.utils.NumassIntegrator;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Нотация простая - начальная энергия всегда слева, конечная всегда справа
 *
 * @author Darksnake
 */
class LossResConvolution implements BivariateFunction {

    BivariateFunction loss;
    BivariateFunction res;

    LossResConvolution(BivariateFunction loss, BivariateFunction res) {
        this.loss = loss;
        this.res = res;
    }

    @Override
    public double value(final double Ein, final double U) {
        UnivariateFunction integrand = (double Eout) -> loss.value(Ein, Eout) * res.value(Eout, U);
        //Энергия в принципе не может быть больше начальной и меньше напряжения
        return NumassIntegrator.getDefaultIntegrator().integrate(U, Ein, integrand);

    }
}
