package hep.dataforge.maths.extensions

import spock.lang.Specification

/**
 * Created by darksnake on 06-Nov-16.
 */
class NumberExtensionTest extends Specification {
    def "test"(){
        given:

        def vec1 = 4d + [2,2].asVector();

        when:
        def res = 0.5*vec1;

        then:

        res as double[] == [3,3]
    }
}
