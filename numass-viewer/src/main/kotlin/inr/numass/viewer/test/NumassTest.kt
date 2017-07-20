package inr.numass.viewer.test

import hep.dataforge.context.Global
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassStorageFactory
import inr.numass.viewer.NumassLoaderView
import javafx.application.Application
import javafx.stage.Stage
import tornadofx.*
import java.io.File

/**
 * Created by darksnake on 17-Jul-17.
 */
class NumassTest : App(NumassLoaderView::class) {
    override fun start(stage: Stage) {
        super.start(stage)
        val storage = NumassStorageFactory.buildLocal(File("D:\\Work\\Numass\\data\\2017_05\\"))
        Global.setDefaultContext(Global.instance())
        val view = find<NumassLoaderView>();
        view.data = storage.provide("Fill_1/set_4", NumassSet::class.java).get();
    }
}

fun main(args: Array<String>) {
    Application.launch(NumassTest::class.java)
}