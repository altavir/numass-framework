/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.fx.test

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXWebOutput
import hep.dataforge.meta.buildMeta
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import tornadofx.*
import java.time.Instant

/**
 *
 * @author Alexander Nozik
 */
class FXWebViewTest : App() {

    override fun start(stage: Stage) {

        val out = FXWebOutput(Global)
        out.render("This is my text")
        out.render(
                buildMeta {
                    "a" to 232
                    "b" to "my string"
                    "node" to {
                        "c" to listOf(1,2,3)
                        "d" to Instant.now()
                    }
                }
        )

        val scene = Scene(out.view.root, 400.0, 400.0)

        stage.title = "FXOutputPaneTest"
        stage.scene = scene
        stage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(FXWebViewTest::class.java, *args)
}
