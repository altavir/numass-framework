package inr.numass.viewer

import hep.dataforge.fx.meta.MetaViewer
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import tornadofx.*
import tornadofx.controlsfx.borders

class PointInfoView(val point: NumassPoint) : MetaViewer(point.meta) {
    private val count: Int by lazy {
        PointCache[point].sumBy { it.getValue(NumassAnalyzer.COUNT_KEY).int }
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
