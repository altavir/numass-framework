package hep.dataforge.maths.tables

import spock.lang.Specification

/**
 * Created by darksnake on 27-Oct-15.
 */
class GTableTest extends Specification {

    def "test table row reading"() {
        given:
        GTable table = new GTable();
        table["a-column"] = [1, 2, 3]
        table["b-column"] = ["some", "text", "here"]
        table[2] = [0, 0, 0, 0]

        when:
        GRow third = table.row(2)

        then:
        third[1] == "here"
    }

    def "test table double index reading"() {
        when:
        GTable table = new GTable();
        table["a-column"] = [1, 2, 3]
        table["b-column"] = ["some", "text", "here"]
        table[2] = [0, 0, 0, 0]

        then:
        table["a-column"][1] == 2
    }
}
