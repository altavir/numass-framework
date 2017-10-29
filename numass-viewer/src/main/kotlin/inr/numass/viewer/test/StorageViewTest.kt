package inr.numass.viewer.test

import inr.numass.viewer.StorageView
import javafx.application.Application
import tornadofx.*

class ViewerTestApp : App(StorageView::class)

fun main(args: Array<String>) {
    Application.launch(ViewerTestApp::class.java, *args);
}