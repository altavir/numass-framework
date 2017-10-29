package inr.numass.viewer.test

import hep.dataforge.kodex.fx.dfIcon
import hep.dataforge.tables.Table
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassStorageFactory
import inr.numass.viewer.AmplitudeView
import inr.numass.viewer.HVView
import inr.numass.viewer.SpectrumView
import javafx.application.Application
import javafx.scene.image.ImageView
import tornadofx.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

class ViewerComponentsTestApp : App(ViewerComponentsTest::class)

class ViewerComponentsTest : View(title = "Numass viewer test", icon = ImageView(dfIcon)) {

    //val rootDir = File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    //val set: NumassSet = NumassStorageFactory.buildLocal(rootDir).provide("loader::set_8", NumassSet::class.java).orElseThrow { RuntimeException("err") }


    private val cache: MutableMap<NumassPoint, Table> = ConcurrentHashMap();

    val amp = AmplitudeView(cache = cache)
    val sp = SpectrumView(cache = cache)
    val hv = HVView()

    override val root = borderpane {
        top {
            button("Click me!") {
                action {
                    runAsync {
                        val rootDir = File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")
                        val set: NumassSet = NumassStorageFactory.buildLocal(rootDir).provide("loader::set_2", NumassSet::class.java)
                                .orElseThrow { RuntimeException("err") }
                        update(set);
                    }
                }
            }
        }
        center {
            tabpane {
                tab("amplitude", amp.root)
                tab("spectrum", sp.root)
                tab("hv", hv.root)
            }
        }
    }

    fun update(set: NumassSet) {
        amp.setAll(set.points.filter { it.voltage != 16000.0 }.collect(Collectors.toMap({ "point_${it.voltage}" }, { it })));
        sp.add("test", set);
        hv.update(set)
    }
}


fun main(args: Array<String>) {
    Application.launch(ViewerComponentsTestApp::class.java, *args);
}