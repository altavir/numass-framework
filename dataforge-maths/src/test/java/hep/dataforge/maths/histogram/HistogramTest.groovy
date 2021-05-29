package hep.dataforge.maths.histogram

import org.apache.commons.math3.random.JDKRandomGenerator
import spock.lang.Specification

import java.util.stream.DoubleStream

import static spock.util.matcher.HamcrestMatchers.closeTo
import static spock.util.matcher.HamcrestSupport.expect

/**
 * Created by darksnake on 02.07.2017.
 */
class HistogramTest extends Specification {

    def testUnivariate() {
        given:
        def histogram = new UnivariateHistogram(-5, 5, 0.1)
        def generator = new JDKRandomGenerator(2234);

        when:
        histogram.fill(DoubleStream.generate { generator.nextGaussian()  }.limit(200000))
        double average = histogram.binStream()
                .filter{!it.getLowerBound(0).infinite && !it.getUpperBound(0).infinite}
                .mapToDouble{
                    //println it.describe()
                    return (it.getLowerBound(0) + it.getUpperBound(0)) / 2d * it.count
                }
                .average()
                .getAsDouble()
        then:
        expect average, closeTo(0,3)
    }
}
