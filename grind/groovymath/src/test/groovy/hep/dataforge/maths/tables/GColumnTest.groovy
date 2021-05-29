package hep.dataforge.maths.tables

/**
 * Created by darksnake on 26-Oct-15.
 */
class GColumnTest extends spock.lang.Specification {
    def "test transform"() {
        given:
            GColumn a = new GColumn([1, 2, 3])
        when:
            def aTrans = a.transform {value, index -> "value" + value}
        then:
            aTrans[1] == "value2"
    }

    def "test plus"() {
        given:
            GColumn a = new GColumn([1, 2, 3])
            GColumn b = new GColumn([2, 2, 2])
        when:
            def c = a+b
        then:
            c[2] == 5
    }
}
