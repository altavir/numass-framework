package hep.dataforge.grind.extensions

import hep.dataforge.grind.Grind
import hep.dataforge.meta.Meta
import spock.lang.Specification

class CoreExtensionTest extends Specification {

    def "Property read"() {
        when:
        Meta meta = Grind.buildMeta(a: 22, b: "asdfg") {
            child(c: 22.8, d: "hocus-pocus")
        }
        then:
        meta.child["c"] == 22.8
        meta.getValue("child.d").string == "hocus-pocus"
    }

    def "Property write"() {
        given:
        Meta meta = Grind.buildMeta(a: 22, b: "asdfg")
        when:
        meta.child = Grind.buildMeta(b: 33)
        then:
        meta["child.b"] == 33
        meta.child["b"] == 33
    }

}
