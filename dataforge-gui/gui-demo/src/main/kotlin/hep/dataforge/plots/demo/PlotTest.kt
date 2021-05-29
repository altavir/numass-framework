/*
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.plots.demo

import hep.dataforge.buildContext
import hep.dataforge.configure
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.fx.plots.group
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.data.XYFunctionPlot
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.plots.output.plotFrame
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Tables
import hep.dataforge.values.ValueMap
import hep.dataforge.values.Values
import java.util.*
import kotlin.concurrent.thread


/**
 * @param args the command line arguments
 */

fun main() {

    val context = buildContext("TEST", JFreeChartPlugin::class.java) {
        output = FXOutputManager()
    }

    val func = { x: Double -> Math.pow(x, 2.0) }

    val funcPlot = XYFunctionPlot.plot("func", 0.1, 4.0, 200, function = func)


    val names = arrayOf("myX", "myY", "myXErr", "myYErr")

    val data = ArrayList<Values>()
    data.add(ValueMap.of(names, 0.5, 0.2, 0.1, 0.1))
    data.add(ValueMap.of(names, 1.0, 1.0, 0.2, 0.5))
    data.add(ValueMap.of(names, 3.0, 7.0, 0, 0.5))
    val ds = Tables.infer(data)

    val dataPlot = DataPlot.plot("data.Plot", ds, Adapters.buildXYAdapter("myX", "myXErr", "myY", "myYErr"))

    context.plotFrame("test", stage = "test") {
        configure {
            "yAxis" to {
                "type" to "log"
            }
        }
        thread {
            Thread.sleep(5000)
            +dataPlot
        }
        +funcPlot
        group("sub") {
            +funcPlot
            +dataPlot
        }
    }

    context.plotFrame("test1") {
        +funcPlot
    }



}


