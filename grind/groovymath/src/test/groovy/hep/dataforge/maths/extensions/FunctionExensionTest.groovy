package hep.dataforge.maths.extensions

import org.apache.commons.math3.analysis.UnivariateFunction
import spock.lang.Specification

/**
 * Created by darksnake on 06-Nov-15.
 */
class FunctionExensionTest extends Specification {
    def "testing function plus construction"() {
        when:
        UnivariateFunction f = { x -> x*x };
        UnivariateFunction g = { x -> 2*x };
        def h = f - g + 1
        then:
        h(2) == 1
    }

    def "testing combination construction"() {
        when:
        UnivariateFunction f = { x -> x };
        UnivariateFunction g = { x -> 2*x };
        def h = f*g
        then:
        h(3) == 18
    }
}
