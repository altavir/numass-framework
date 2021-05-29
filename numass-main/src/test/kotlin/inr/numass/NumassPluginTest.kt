package inr.numass

import hep.dataforge.context.Global
import hep.dataforge.meta.buildMeta
import hep.dataforge.stat.fit.FitManager
import org.junit.Before
import org.junit.Test

class NumassPluginTest {
    @Before
    fun setup() {
        NumassPlugin().startGlobal()
    }

    @Test
    fun testModels() {
        val meta = buildMeta("model") {
            "modelName" to "sterile"
            "resolution" to {
                "width" to 8.3e-5
                "tail" to "function::numass.resolutionTail.2017.mod"
            }
            "transmission" to {
                "trapping" to "function::numass.trap.nominal"
            }
        }
        val model = Global.load<FitManager>().buildModel(meta)
    }
}

