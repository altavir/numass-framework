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
package hep.dataforge.stat

import hep.dataforge.maths.chain.Chain
import hep.dataforge.maths.chain.SimpleChain
import hep.dataforge.maths.domains.Domain
import org.apache.commons.math3.exception.TooManyEvaluationsException
import org.apache.commons.math3.random.RandomGenerator
import org.apache.commons.math3.random.RandomVectorGenerator


/**
 *
 * UniformRandomVectorGenerator class.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
class UniformRandomVectorGenerator @JvmOverloads constructor(
        val domain: Domain,
        val generator: RandomGenerator = defaultGenerator,
        val maxRejections: Int? = 20) : RandomVectorGenerator {

    private fun next(): DoubleArray {
        val res = DoubleArray(domain.dimension)
        for (i in res.indices) {
            val a = domain.getLowerBound(i)
            val b = domain.getUpperBound(i)
            assert(b >= a)
            res[i] = a + generator.nextDouble() * (b - a)

        }
        return res
    }

    /**
     * {@inheritDoc}
     */
    override fun nextVector(): DoubleArray {
        var res = this.next()
        var i = 0
        while (!domain.contains(res)) {
            res = this.next()
            i++
            if (maxRejections!=null && i >= maxRejections) {
                throw TooManyEvaluationsException(maxRejections)
            }
        }
        return res
    }
}

val RandomVectorGenerator.chain: Chain<DoubleArray>
    get() {
        return SimpleChain { nextVector() }
    }
