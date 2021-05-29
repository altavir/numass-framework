package hep.dataforge.maths


import org.apache.commons.math3.linear.Array2DRowRealMatrix
import spock.lang.Specification

/**
 * Created by darksnake on 14.06.2017.
 */
class NamedMatrixTest extends Specification {
    def "MetaMorph"() {
        given:
        def matrix = new NamedMatrix(["a", "b"] as String[], new Array2DRowRealMatrix([[1d, 2d], [-2d, 3d]] as double[][]))
        when:
        def meta = matrix.toMeta();
        def newMatrix = MetaMorph.morph(NamedMatrix.class, meta);
        then:
        newMatrix.get(0, 1) == 2
        newMatrix.get(1, 1) == 3
    }
}
