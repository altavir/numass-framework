/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.fx.test

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXTextOutput
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import tornadofx.*

/**
 *
 * @author Alexander Nozik
 */
class FXOutputPaneTest : App() {

    override fun start(stage: Stage) {

        val out = FXTextOutput(Global)
        out.setMaxLines(5)

        for (i in 0..11) {
            out.appendLine("my text number $i")
        }

        //        onComplete.appendLine("a\tb\tc");
        //        onComplete.appendLine("aaaaa\tbbb\tccc");

        val scene = Scene(out.view.root, 400.0, 400.0)

        stage.title = "FXOutputPaneTest"
        stage.scene = scene
        stage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(FXOutputPaneTest::class.java, *args)
}
