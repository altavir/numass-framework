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
package hep.dataforge.maths.integration;

import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * An integrand using commons math accuracy notation
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class CMIntegrand extends UnivariateIntegrand {

    private double absoluteAccuracy = Double.POSITIVE_INFINITY;
    private double relativeAccuracy = Double.POSITIVE_INFINITY;
    private int iterations = 0;

    public CMIntegrand(Double lower, Double upper, UnivariateFunction function) {
        super(lower, upper, function);
    }

    public CMIntegrand(double absoluteAccuracy, double relativeAccuracy, int iterations, int numCalls, Double value, UnivariateIntegrand integrand) {
        super(integrand, numCalls, value);
        this.absoluteAccuracy = absoluteAccuracy;
        this.relativeAccuracy = relativeAccuracy;
        this.iterations = iterations;
    }

    public double getAbsoluteAccuracy() {
        return absoluteAccuracy;
    }

    public int getIterations() {
        return iterations;
    }

    public double getRelativeAccuracy() {
        return relativeAccuracy;
    }

}
