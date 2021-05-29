package hep.dataforge.maths

import org.apache.commons.math3.analysis.UnivariateFunction
import spock.lang.Specification

/**
 * Created by darksnake on 25-Nov-16.
 */
class GMTest extends Specification {
    def "Function"() {
        when:
        def f = 1 + GM.function { x -> x**2 } + { x -> 2 * x };
        then:
        f(1) == 4
    }

    def "test closure function"() {
        given:
        UnivariateFunction func = { x -> x**2 };
        when:
        def newFunc = 1 + func ;
        then:
        newFunc(2) == 5;
    }
}
