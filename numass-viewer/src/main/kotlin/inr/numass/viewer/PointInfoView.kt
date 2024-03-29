package inr.numass.viewer

import hep.dataforge.fx.meta.MetaViewer
import inr.numass.data.analyzers.NumassAnalyzer
import javafx.beans.property.SimpleIntegerProperty
import kotlinx.coroutines.launch
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*
import tornadofx.controlsfx.borders
import tornadofx.controlsfx.toGlyph

class PointInfoView(val cachedPoint: DataController.CachedPoint) : MetaViewer(cachedPoint.meta) {
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
                            app.context.launch {
                                val res = cachedPoint.spectrum.await().sumOf {
                                    it.getValue(NumassAnalyzer.COUNT_KEY).int
                                }
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
                            textProperty().bind(countProperty.stringBinding {
                                String.format("%.2f",
                                    it!!.toDouble() / cachedPoint.length.toMillis() * 1000)
                            })
                        }
                    }
                }
            }
        }
    }
}
