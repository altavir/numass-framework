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

private val analyzer = SimpleAnalyzer()


class CachedPoint(val point: NumassPoint) : NumassPoint by point {

    override val blocks: List<NumassBlock> by lazy { point.blocks }

    override val meta: Meta = point.meta

    val channelSpectra: Deferred<Map<Int, Table>> = GlobalScope.async(start = CoroutineStart.LAZY) {
        point.channels.mapValues { (_, value) -> analyzer.getAmplitudeSpectrum(value) }
    }

    val spectrum: Deferred<Table> = GlobalScope.async(start = CoroutineStart.LAZY) { analyzer.getAmplitudeSpectrum(point) }
}

class CachedSet(set: NumassSet) : NumassSet by set {
    override val points: List<CachedPoint> by lazy { set.points.map { CachedPoint(it) } }
}
