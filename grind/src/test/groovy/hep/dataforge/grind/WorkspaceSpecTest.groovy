package hep.dataforge.grind

import hep.dataforge.meta.Meta
import hep.dataforge.workspace.Workspace
import spock.lang.Specification

/**
 * Created by darksnake on 04-Aug-16.
 */
class WorkspaceSpecTest extends Specification {

    def "Test meta builder delegation"() {
        given:
        def closure = {
            myMeta(myPar: "val", myOtherPar: 28) {
                childNode(childValue: true)
                otherChildNode {
                    grandChildNode(grandChildValue: 88.6)
                }
            }
        }
        when:
        def metaSpec = new GrindMetaBuilder()
        def metaExec = closure.rehydrate(metaSpec, this, this);
        metaExec.resolveStrategy = Closure.DELEGATE_ONLY;
        def res = metaExec()
        then:
//            println res.getString("otherChildNode.grandChildNode.grandChildValue")
        res.getBoolean("childNode.childValue");
    }

    def "Test meta from string"() {
        given:
        String metaStr = """
                myMeta(myPar: "val", myOtherPar: 28) {
                    childNode(childValue: true)
                    otherChildNode {
                        grandChildNode(grandChildValue: 88.6)
                    }
                }
        """
        when:
        Meta meta = Grind.parseMeta(metaStr);
        then:
        meta.getName() == "myMeta"
        meta.getDouble("otherChildNode.grandChildNode.grandChildValue") == 88.6
    }

    def "Test task builder"() {
        when:
        Workspace wsp = Grind.buildWorkspace {
            data {
                (1..10).each {
                    item("data_$it", "value_$it")
                }
            }
            task hep.dataforge.grind.workspace.DefaultTaskLib.pipe("test") { String input ->
                if (input.endsWith("5")) {
                    return input + "_hurray!"
                } else {
                    return input
                }
            }

            task hep.dataforge.grind.workspace.DefaultTaskLib.pipe("preJoin") { String input ->
                input[6]
            }

            task hep.dataforge.grind.workspace.DefaultTaskLib.join("testJoin", [dependsOn: "preJoin"]) { Map<String, String> input ->
                input.sort().values().sum()
            }

        }
        def result = wsp.runTask("test", "test").get("data_5")
        def joinResult = wsp.runTask("testJoin", "testJoin").getData().get()
        then:
        result.endsWith("hurray!")
        joinResult == "1123456789"
    }
}
