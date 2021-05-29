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

import hep.dataforge.maths.chain.Chain
import hep.dataforge.maths.chain.SimpleChain
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import org.apache.commons.math3.distribution.MultivariateRealDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.random.RandomGenerator


val RealDistribution.chain: Chain<Double>
    get() {
        return SimpleChain { this.sample() }
    }

val MultivariateRealDistribution.chain: Chain<DoubleArray>
    get() {
        return SimpleChain { this.sample() }
    }

/**
 * @param proposal chain generator for initial proposal distribution
 * @param proposalDensity probability density for proposal distribution
 * @param targetDensity target probability density
 */
fun <T : Any> rejectingChain(
    proposal: Chain<T>,
    proposalDensity: (T) -> Double,
    factor: Double = 1.0,
    generator: RandomGenerator = defaultGenerator,
    targetDensity: (T) -> Double,
): Chain<T> {
    return SimpleChain {
        //TODO check if target density higher than proposal density?
        proposal.flow.dropWhile { generator.nextDouble() < targetDensity(it) / proposalDensity(it) / factor }.first()
    }
}

/**
 * Sample given distribution using this disribution and accept-reject method
 */
fun RealDistribution.rejectingChain(
    factor: Double = 1.0,
    generator: RandomGenerator = defaultGenerator,
    targetDensity: (Double) -> Double,
): Chain<Double> {
    return rejectingChain(this.chain, this::density, factor, generator, targetDensity)
}