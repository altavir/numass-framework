package inr.numass.viewer

import hep.dataforge.fx.meta.MetaViewer
import inr.numass.data.analyzers.NumassAnalyzer
import kotlinx.coroutines.experimental.runBlocking
import tornadofx.*
import tornadofx.controlsfx.borders

class PointInfoView(val point: CachedPoint) : MetaViewer(point.meta) {
    private val count: Int by lazy {
        runBlocking {
            point.spectrum.await().sumBy { it.getValue(NumassAnalyzer.COUNT_KEY).int }
        }
    }

    override val root = super.root.apply {
        top {
            gridpane {
                borders {
                    lineBorder().build()
                }
                row {
                    hbox {
                        label("Total number of events: ")
                        label("$count")
                    }
                }
                row {
                    hbox {
                        label("Total count rate: ")
                        label(String.format("%.2f", count.toDouble() / point.length.toMillis() * 1000))
                    }
                }
            }
        }
    }
}
