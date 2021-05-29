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

package hep.dataforge.fx.test

import hep.dataforge.fx.meta.MetaViewer
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta
import javafx.scene.Scene
import javafx.stage.Stage
import tornadofx.*

class MetaViewerTest : App() {

    val meta = buildMeta("test")
            .setValue("testValue", "[1,2,3]")
            .setValue("anotherTestValue", 15)
            .putNode(MetaBuilder("childNode")
                    .setValue("childValue", true)
                    .setValue("anotherChildValue", 18)
            ).putNode(MetaBuilder("childNode")
                    .setValue("childValue", true)
                    .putNode(MetaBuilder("grandChildNode")
                            .putValue("grandChildValue", "grandChild")
                    )
            ).build()

    override fun start(stage: Stage) {
        val scene = Scene(MetaViewer(meta).root, 400.0, 400.0)

        stage.title = "Meta viewer test"
        stage.scene = scene
        stage.show()
    }
}