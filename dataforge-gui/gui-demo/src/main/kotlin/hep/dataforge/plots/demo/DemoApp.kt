package hep.dataforge.plots.demo

import javafx.application.Application
import tornadofx.*

class DemoApp: App(DemoView::class) {
}

fun main(args: Array<String>) {
    Application.launch(DemoApp::class.java,*args);
}