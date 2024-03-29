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

package inr.numass.scripts.tristan

import hep.dataforge.context.Global
import hep.dataforge.storage.files.FileStorage
import inr.numass.data.api.NumassPoint
import inr.numass.data.storage.NumassDataLoader
import inr.numass.data.storage.NumassDirectory

fun main() {
    val storage = NumassDirectory.read(Global, "D:\\Work\\Numass\\data\\2018_04\\Adiabacity_19\\") as FileStorage
    val set = storage["set_4"] as NumassDataLoader
    set.points.forEach { point ->
        if (point.voltage == 18700.0) {
            println("${point.index}:")
            point.blocks.forEach {
                println("\t${it.channel}: events: ${it.events.count()}, time: ${it.length}")
            }
        }
    }

    val point: NumassPoint = set.points.first { it.index == 18 }
    (0..99).forEach { bin ->
        val times = point.events.filter { it.amplitude > 0U }.map { it.timeOffset }.toList()
        val count = times.count { it.toDouble() > bin.toDouble() / 10 * 1e9 && it.toDouble() < (bin + 1).toDouble() / 10 * 1e9 }
        println("${bin.toDouble() / 10.0}: $count")
    }
}