package inr.numass.viewer

import hep.dataforge.fx.meta.MetaViewer
import inr.numass.data.analyzers.NumassAnalyzer
import javafx.beans.property.SimpleIntegerProperty

class PointInfoView(val point: CachedPoint) : MetaViewer(point.meta) {

    val countProperty = SimpleIntegerProperty(0)
    var count by countProperty


    override val root = super.root.apply {
        top {
            gridpane {
                borders {
                    lineBorder().build()
                }
                row {
                    button(graphic = FontAwesome.Glyph.REFRESH.toGlyph()) {
                        action {
                            GlobalScope.launch {
                                val res = point.spectrum.await().sumBy { it.getValue(NumassAnalyzer.COUNT_KEY).int }
                                runLater { count = res }
                            }
                        }
                    }
                }
                row {
                    hbox {
                        label("Total number of events: ")
                        label {
                            textProperty().bind(countProperty.asString())
                        }
                    }
                }
                row {
                    hbox {
                        label("Total count rate: ")
                        label {
                            textProperty().bind(countProperty.stringBinding { String.format("%.2f", it!!.toDouble() / point.length.toMillis() * 1000) })
                        }
                    }
                }
            }
        }
    }
}
