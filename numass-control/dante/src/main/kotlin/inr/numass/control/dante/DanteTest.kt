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

package inr.numass.control.dante

import hep.dataforge.kodex.buildMeta
import inr.numass.data.channel
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    val client = DanteClient("192.168.111.123", 7)
    val meta = buildMeta {

        /*
               val gain = meta.getDouble("gain")
        val det_thresh = meta.getInt("detection_thresold")
        val pileup_thr = meta.getInt("pileup_thresold")
        val en_fil_peak_time = meta.getInt("energy_filter.peaking_time")
        val en_fil_flattop = meta.getInt("energy_filter.flat_top")
        val fast_peak_time = meta.getInt("fast_filter.peaking_time")
        val fast_flattop = meta.getInt("fast_filter.flat_top")
        val recovery_time = meta.getValue("recovery_time").longValue()
        val zero_peak_rate = meta.getInt("zero_peak_rate")
        val inverted_input = meta.getInt("inverted_input")
         */
    }
    runBlocking {
        client.configureAll(meta)
    }
    val point = runBlocking {
        client.readPoint(10*1000)
    }
    println("***META***")
    println(point.meta)
    println("***BLOCKS***")
    point.blocks.forEach {
        println("channel: ${it.channel}")
        println("\tlength: ${it.length}")
        println("\tevents: ${it.events.count()}")
    }
}