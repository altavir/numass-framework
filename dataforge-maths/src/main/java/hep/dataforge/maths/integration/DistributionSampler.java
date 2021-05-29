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

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.MultivariateRealDistribution;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.fill;

/**
 * <p>DistributionSampler class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class DistributionSampler implements Sampler {

    private MultivariateRealDistribution distr;

    public DistributionSampler(MultivariateRealDistribution distr) {
        this.distr = distr;
    }

    public DistributionSampler(RandomGenerator rng, double[] means, double[][] covariance) {
        assert means.length == covariance.length;
        try {
            this.distr = new MultivariateNormalDistribution(rng, means, covariance);
        } catch (SingularMatrixException ex) {
            // Если ковариационная матрица слишком плохо определена
            double[][] diagonal = new double[means.length][means.length];
            for (int i = 0; i < diagonal.length; i++) {
                fill(diagonal[i], 0);
                diagonal[i][i] = covariance[i][i];
            }
            LoggerFactory.getLogger(getClass()).info("The covariance is singular. Using only diagonal elements.");
            this.distr = new MultivariateNormalDistribution(rng, means, diagonal);
        }
    }

    @Override
    public int getDimension() {
        return distr.getDimension();
    }

    @Override
    public Sample nextSample(Sample previous) {
        double[] sample = distr.sample();
        return new Sample(distr.density(sample), sample);
    }

}
