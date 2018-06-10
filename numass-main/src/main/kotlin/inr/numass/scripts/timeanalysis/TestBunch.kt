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

package inr.numass.scripts.timeanalysis

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.coroutineContext
import hep.dataforge.kodex.generate
import hep.dataforge.kodex.join
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import inr.numass.NumassPlugin
import inr.numass.actions.TimeAnalyzerAction
import inr.numass.data.NumassGenerator
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.generateBlock
import inr.numass.data.withDeadTime
import java.time.Instant

fun main(args: Array<String>) {
    Global.output = FXOutputManager()
    JFreeChartPlugin().startGlobal()
    NumassPlugin().startGlobal()

    val cr = 3.0
    val length = (30000 *1e9).toLong()
    val num = 1
    val dt = 6.5

    val start = Instant.now()

    val point = (1..num).map {
        Global.generate {
            val events = NumassGenerator
                    .generateEvents(cr)

            val bunches = NumassGenerator
                    .generateBunches(6.0, 0.001, 5.0)

            val discharges = NumassGenerator
                    .generateBunches(50.0,0.001,0.1)

            NumassGenerator.mergeEventChains(events, bunches, discharges).withDeadTime { (dt * 1000).toLong() }.generateBlock(start.plusNanos(it * length), length)
        }
    }.join(Global.coroutineContext) { blocks ->
        SimpleNumassPoint(blocks, 18000.0)
    }.get()


    val meta = buildMeta {
        "analyzer" to {
            "t0" to 30000
        }
        "binNum" to 200
    }

    TimeAnalyzerAction().simpleRun(point, meta);
}