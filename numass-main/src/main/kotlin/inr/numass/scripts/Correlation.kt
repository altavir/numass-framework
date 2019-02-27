/*
 * Copyright  2017 Alexander Nozik.
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

package inr.numass.scripts

import hep.dataforge.buildContext
import hep.dataforge.meta.buildMeta
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDirectory
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation


private fun correlation(sequence: List<NumassEvent>): Double {
    val amplitudes: MutableList<Double> = ArrayList()
    val times: MutableList<Double> = ArrayList()
    sequence.forEach {
        amplitudes.add(it.amplitude.toDouble())
        times.add(it.timeOffset.toDouble())
    }

    return PearsonsCorrelation().correlation(amplitudes.toDoubleArray(), times.toDoubleArray())
}

fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java) {
        rootDir = "D:\\Work\\Numass\\sterile\\2017_05"
        dataDir = "D:\\Work\\Numass\\data\\2017_05"
    }
    //val rootDir = File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    val storage = NumassDirectory.read(context, "Fill_2")!!

    val sets = (2..14).map { "set_$it" }

    val loaders = sets.mapNotNull { set ->
        storage.provide("loader::$set", NumassSet::class.java).orElse(null)
    }

    val set = NumassDataUtils.join("sum", loaders)

    val analyzer = SmartAnalyzer();

    val meta = buildMeta {
        "window.lo" to 400
        "window.up" to 2500
    }

    println("Correlation between amplitudes and delays:")
    set.points.filter { it.voltage < 16000.0 }.forEach {
        val cor = correlation(analyzer.getEvents(it, meta))
        println("${it.voltage}: $cor")
    }

}