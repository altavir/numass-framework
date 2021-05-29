package hep.dataforge.grind.workspace

import hep.dataforge.context.Global
import org.junit.Test

import static org.junit.Assert.assertEquals

class WorkspaceTest {

    def workspace = new WorkspaceSpec(Global.INSTANCE).with {

        context {
            name = "TEST"
        }

        data {
            item("xs") {
                meta(axis: "x")
                (1..100).asList() //generate xs
            }
            node("ys") {
                Random rnd = new Random()
                item("y1") {
                    meta(axis: "y")
                    (1..100).collect { it**2 }
                }
                item("y2") {
                    meta(axis: "y")
                    (1..100).collect { it**2 + rnd.nextDouble() }
                }
                item("y3") {
                    meta(axis: "y")
                    (1..100).collect { (it + rnd.nextDouble() / 2)**2 }
                }
            }
        }

//        task custom("dif") {
//            def xs = data.optData("xs").get()
//            def ys = data.getNode("ys")
//            ys.dataStream().forEach {
//                yield it.name, combine(it, xs, Table.class, it.meta) { x, y ->
//                    ListTable.Builder
//                }
//            }
//        }

//        task pipe("plot", data: "*") {
//            //PlotManager pm = context.getFeature(PlotManager)//loading plot feature
//            def helper = new PlotHelper(context)
//            helper.plot((1..100), input as List, name as String)
//        }

        it.builder.build()
    }


    @Test
    void testData() {
        assertEquals(3, workspace.data.getNode("ys").getSize())
    }
}
