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

package inr.numass.viewer

import hep.dataforge.meta.Meta
import hep.dataforge.tables.Table
import inr.numass.data.analyzers.SimpleAnalyzer
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

private val analyzer = SimpleAnalyzer()


class CachedPoint(point: NumassPoint) : NumassPoint by point {
    private val lazyBlocks: () -> List<NumassBlock> = { point.blocks }

    override val blocks: List<NumassBlock>
        get() = lazyBlocks()

    override val meta: Meta = point.meta

    val channelSpectra: Deferred<Map<Int, Table>> = async(start = CoroutineStart.LAZY) {
        return@async point.channels.mapValues { (_, value) -> analyzer.getAmplitudeSpectrum(value) }
    }

    val spectrum: Deferred<Table> = async(start = CoroutineStart.LAZY) {
        analyzer.getAmplitudeSpectrum(point)
    }
}

class CachedSet(set: NumassSet) : NumassSet by set {
    override val points: List<CachedPoint> = set.points.map { CachedPoint(it) }
}