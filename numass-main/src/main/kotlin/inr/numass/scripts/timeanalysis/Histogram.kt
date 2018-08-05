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

import hep.dataforge.buildContext
import hep.dataforge.maths.histogram.SimpleHistogram
import hep.dataforge.meta.buildMeta
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassSet
import kotlin.streams.asStream

fun main(args: Array<String>) {
    val context = buildContext("NUMASS", NumassPlugin::class.java) {
        rootDir = "D:\\Work\\Numass\\sterile2018_04"
        dataDir = "D:\\Work\\Numass\\data\\2018_04"
    }

    val storage = NumassStorageFactory.buildLocal(context, "Fill_4", true, false);

    val meta = buildMeta {
        "t0" to 3000
        "chunkSize" to 3000
        "mean" to TimeAnalyzer.AveragingMethod.ARITHMETIC
        //"separateParallelBlocks" to true
        "window" to {
            "lo" to 0
            "up" to 4000
        }
        //"plot.showErrors" to false
    }

    //def sets = ((2..14) + (22..31)).collect { "set_$it" }
    val sets = (2..12).map { "set_$it" }
    //def sets = (16..31).collect { "set_$it" }
    //def sets = (20..28).collect { "set_$it" }

    val loaders = sets.map { set ->
        storage.provide("loader::$set", NumassSet::class.java).orElse(null)
    }.filter { it != null }

    val joined = NumassDataUtils.join("sum", loaders)

    val hv = 14000.0

    val point = joined.first { it.voltage == hv }

    val histogram = SimpleHistogram(arrayOf(0.0, 0.0), arrayOf(20.0, 100.0))
    histogram.fill(
            TimeAnalyzer().getEventsWithDelay(point, meta)
                    .filter { it.second <10000 }
                    .filter { it.first.channel == 0 }
                    .map { arrayOf(it.first.amplitude.toDouble(), it.second.toDouble()/1e3) }
                    .asStream()
    )

    histogram.binStream().forEach {
        println("${it.getLowerBound(0)}\t${it.getLowerBound(1)}\t${it.count}")
    }

}