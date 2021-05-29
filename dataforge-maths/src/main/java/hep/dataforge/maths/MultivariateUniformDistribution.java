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
package hep.dataforge.maths;

import hep.dataforge.maths.domains.Domain;
import hep.dataforge.maths.domains.HyperSquareDomain;
import kotlin.Pair;
import org.apache.commons.math3.distribution.AbstractMultivariateRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.List;

/**
 * A uniform distribution in a
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class MultivariateUniformDistribution extends AbstractMultivariateRealDistribution {

    /**
     * Create a uniform distribution with hyper-square domain
     *
     * @param rg
     * @param loVals
     * @param upVals
     * @return
     */
    public static MultivariateUniformDistribution square(RandomGenerator rg, Double[] loVals, Double[] upVals) {
        return new MultivariateUniformDistribution(rg, new HyperSquareDomain(loVals, upVals));
    }

    public static MultivariateUniformDistribution square(RandomGenerator rg, List<Pair<Double, Double>> borders) {
        Double[] loVals = new Double[borders.size()];
        Double[] upVals = new Double[borders.size()];
        for (int i = 0; i < borders.size(); i++) {
            loVals[i] = borders.get(i).getFirst();
            upVals[i] = borders.get(i).getSecond();
        }
        return new MultivariateUniformDistribution(rg, new HyperSquareDomain(loVals, upVals));
    }

    private Domain domain;

    public MultivariateUniformDistribution(RandomGenerator rg, Domain dom) {
        super(rg, dom.getDimension());
        this.domain = dom;
    }

    @Override
    public double density(double[] doubles) {
        if (doubles.length != this.getDimension()) {
            throw new IllegalArgumentException();
        }
        if (!domain.contains(doubles)) {
            return 0;
        }
        return 1 / domain.volume();
    }

    public double getVolume() {
        return domain.volume();
    }

    @Override
    public double[] sample() {
        double[] res = new double[this.getDimension()];

        do {
            for (int i = 0; i < res.length; i++) {
                double loval = domain.getLowerBound(i);
                double upval = domain.getUpperBound(i);
                if (loval == upval) {
                    res[i] = loval;
                } else {
                    res[i] = loval + this.random.nextDouble() * (upval - loval);
                }

            }
        } while (!domain.contains(res));

        return res;
    }
}
