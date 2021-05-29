/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.stat

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * A real distribution $x -> coef * x^{pow}$
 */
class PolynomialDistribution(
        val a: Double,
        val b: Double,
        val pow: Double,
        rng: RandomGenerator = defaultGenerator) : AbstractRealDistribution(rng) {

    val norm = 1.0 / (pow + 1) * (Math.pow(b, pow + 1) - Math.pow(a, pow + 1))

    override fun density(x: Double): Double {
        return if (x in (a..b)) {
            Math.pow(x, pow) / norm
        } else {
            0.0
        }
    }

    override fun cumulativeProbability(x: Double): Double {
        return when {
            x < a -> return 0.0
            x in (a..b) -> (Math.pow(x, pow + 1) - Math.pow(a, pow + 1)) / (Math.pow(b, pow + 1) - Math.pow(a, pow + 1))
            else -> return 1.0
        }
    }

    override fun getNumericalMean(): Double {
        return (Math.pow(b, pow + 2) - Math.pow(a, pow + 2)) / norm / (pow + 2)
    }

    override fun getNumericalVariance(): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isSupportConnected(): Boolean = true

    override fun isSupportLowerBoundInclusive(): Boolean = true

    override fun isSupportUpperBoundInclusive(): Boolean = true

    override fun getSupportLowerBound(): Double = a

    override fun getSupportUpperBound(): Double = b

}