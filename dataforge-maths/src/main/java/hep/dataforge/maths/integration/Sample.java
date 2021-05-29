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

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * A single multivariate sample with weight
 *
 * @author Alexander Nozik

 */
public class Sample {

    private final double weight;
    private final RealVector sample;

    public Sample(double weight, RealVector sample) {
        this.weight = weight;
        this.sample = sample;
    }

    public Sample(double weight, double[] sample) {
        this.weight = weight;
        this.sample = new ArrayRealVector(sample);
    }

    public int getDimension() {
        return sample.getDimension();
    }

    public double getWeight() {
        return weight;
    }

    public RealVector getVector() {
        return sample;
    }

    public double[] getArray() {
        return sample.toArray();
    }
}
