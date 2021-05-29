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

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.io.render
import hep.dataforge.meta.Meta
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.output.plotFrame
import hep.dataforge.tables.buildTable

fun main() {

    Global.output = FXOutputManager()

    Global.output["text1", "nf"].render("affff")
    Global.output["text1", "n"].render("affff")
    Global.output["text2", "n"].render("affff")


    Global.plotFrame(stage = "plots", name = "frame") {
        DataPlot.plot("data", x = doubleArrayOf(1.0, 2.0, 3.0), y = doubleArrayOf(2.0, 3.0, 4.0))
    }

    val table = buildTable {
        row("a" to 1, "b" to 2)
        row("a" to 2, "b" to 4)
    }

    Global.output.render(table, stage = "tables", name = "table", meta = Meta.empty())

    System.`in`.read()
}