package hep.dataforge.maths.integration

import kotlin.Pair
import org.apache.commons.math3.analysis.MultivariateFunction
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.random.JDKRandomGenerator
import spock.lang.Specification

import static spock.util.matcher.HamcrestMatchers.closeTo

/**
 * Created by darksnake on 06.07.2017.
 */
class MonteCarloIntegratorTest extends Specification {

    //triangular function
    MultivariateFunction function = { pars ->
        def x = pars[0];
        def y = pars[1];
        if (x >= -1 && y <= 1 && y >= x) {
            return 1d
        } else {
            return 0d
        }
    }
    def integrator = new MonteCarloIntegrator();
    def generator = new JDKRandomGenerator(12345)


    def testUniform(){
        Sampler sampler = Sampler.uniform(generator, [new Pair<>(-1d,1d), new Pair<>(-1d,1d)]);
        def integrand = new MonteCarloIntegrand(sampler,function);
        def res = integrator.integrate(integrand)
        expect:
        res closeTo(2d,0.05d);
    }

    def testNormal(){
        Sampler sampler = Sampler.normal(generator, new ArrayRealVector([0d,0d] as double[]), new
                Array2DRowRealMatrix([[1d,0d],[0d,1d]] as double[][]));
        def integrand = new MonteCarloIntegrand(sampler,function);
        def res = integrator.integrate(integrand)
        expect:
        res closeTo(2d,0.05d);
    }
}
