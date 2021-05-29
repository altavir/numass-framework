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

import hep.dataforge.maths.MultivariateUniformDistribution;
import kotlin.Pair;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <p>Abstract Sampler class.</p>
 *
 * @author Alexander Nozik
 */
public interface Sampler {

    static Sampler uniform(RandomGenerator generator, List<Pair<Double, Double>> borders) {
        return new DistributionSampler(MultivariateUniformDistribution.square(generator, borders));
    }

    static Sampler normal(RandomGenerator generator, RealVector means, RealMatrix covariance){
        return new DistributionSampler(new MultivariateNormalDistribution(generator,means.toArray(),covariance.getData()));
    }


    Sample nextSample(@Nullable Sample previousSample);

//    default Stream<Sample> stream() {
//        return Stream.generate(this::nextSample);
//    }

    int getDimension();
}
