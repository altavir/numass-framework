package hep.dataforge.grind.workspace

import hep.dataforge.context.Global
import hep.dataforge.data.DataSet
import hep.dataforge.grind.Grind
import hep.dataforge.meta.Meta
import spock.lang.Specification
import spock.lang.Timeout

class ExecTest extends Specification {

    @Timeout(3)
    def "get Java version"() {
        given:
        def exec = new ExecSpec()
        exec.with{
            cli {
                append "java"
                append "-version"
            }
            output {
                println "Out: " + out
                println "Err: " + err
                return err.split()[0]
            }
        }
        def action = exec.build()
        when:
        def res = action.simpleRun("test")
        then:
        res == "java"
    }

    @Timeout(5)
    def "run python script"(){
        given:
        def exec = new ExecSpec()
        exec.with{
            cli {
                append "python"
                argument  context.getClassLoader().getResource('workspace/test.py')
                append "-d 1"
                append "-r ${meta["result"]}"
            }
        }
        when:
        def meta = Grind.buildMeta(result: "OK")
        def res = exec.build().simpleRun("test", meta);
        println "Result: $res"
        then:
        res.trim().endsWith "OK"
    }

    @Timeout(5)
    def "parallel test"(){
        given:
        def exec = new ExecSpec()
        exec.with{
            cli {
                append "python"
                argument  context.getClassLoader().getResource('workspace/test.py')
                append "-d 1"
                append "-r $name: ${meta["result"]}"
            }
        }
        when:
        def builder = DataSet.edit(Object)
        (1..8).each {
            builder.putData("test$it","test$it",Grind.buildMeta(result: "OK$it"))
        }
        def res = exec.build().run(Global.INSTANCE, builder.build(), Meta.empty())
        then:
        res.computeAll()
        res.getSize() == 8
        res["test4"].get().trim().endsWith("OK4")
    }
}
