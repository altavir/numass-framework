package hep.dataforge.stat

import hep.dataforge.maths.chain.Chain
import hep.dataforge.maths.domains.Domain
import hep.dataforge.maths.domains.RangeDomain
import hep.dataforge.maths.domains.UnconstrainedDomain
import hep.dataforge.maths.domains.UnivariateDomain
import org.apache.commons.math3.distribution.MultivariateRealDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.random.RandomGenerator

interface Distribution<T> {
    val density: (T) -> Double
    val domain: Domain
    val dimension: Int
        get() = domain.dimension
    fun getChain(gen: RandomGenerator = defaultGenerator): Chain<T>
}

interface UnivariateDistribution : Distribution<Double> {
    override val domain: UnivariateDomain
}

class CMUnivariateDistribution(val distribution: RealDistribution) : UnivariateDistribution {
    override val density: (Double) -> Double = distribution::density

    override fun getChain(gen: RandomGenerator): Chain<Double> {
        //FIXME currently generator is ignored. Will be fixed in CM4
        return distribution.chain
    }

    override val domain: UnivariateDomain = RangeDomain(distribution.supportLowerBound, distribution.supportUpperBound)
}

class CMMultivariateDistribution(val distribution: MultivariateRealDistribution) : Distribution<DoubleArray> {
    override val density: (DoubleArray) -> Double = distribution::density

    override val domain: Domain = UnconstrainedDomain(distribution.dimension);

    override fun getChain(gen: RandomGenerator): Chain<DoubleArray> {
        //FIXME currently generator is ignored. Will be fixed in CM4
        return distribution.chain
    }
}
