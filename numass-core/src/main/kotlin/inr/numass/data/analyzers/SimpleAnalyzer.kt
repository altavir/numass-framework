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

package inr.numass.data.analyzers

import hep.dataforge.meta.Meta
import hep.dataforge.tables.ValueMap
import hep.dataforge.values.Values
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.SignalProcessor

/**
 * A simple event counter
 * Created by darksnake on 07.07.2017.
 */
class SimpleAnalyzer @JvmOverloads constructor(private val processor: SignalProcessor? = null) : AbstractAnalyzer(processor) {


    override fun analyze(block: NumassBlock, config: Meta): Values {
        val loChannel = config.getInt("window.lo", 0)
        val upChannel = config.getInt("window.up", Integer.MAX_VALUE)
        val count = getEvents(block, config).count()
        val length = block.length.toNanos().toDouble() / 1e9
        val countRate = count.toDouble() / length
        val countRateError = Math.sqrt(count.toDouble()) / length

        return ValueMap.of(AbstractAnalyzer.NAME_LIST,
                length,
                count,
                countRate,
                countRateError,
                arrayOf(loChannel, upChannel),
                block.startTime)
    }


}
