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

import hep.dataforge.tables.Table
import hep.dataforge.utils.Misc
import inr.numass.data.analyzers.SimpleAnalyzer
import inr.numass.data.api.NumassPoint
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import tornadofx.*


private val analyzer = SimpleAnalyzer()


class PointCache : Controller() {
    private val context = app.context

    inner class CachedPoint(point: NumassPoint) {
        val length = point.length

        val voltage = point.voltage

        val meta = point.meta

        val channelSpectra: Deferred<Map<Int, Table>> = context.async(Dispatchers.IO) {
            point.channels.mapValues { (_, value) -> analyzer.getAmplitudeSpectrum(value) }
        }

        val spectrum: Deferred<Table> = context.async(Dispatchers.IO) {
            analyzer.getAmplitudeSpectrum(point)
        }
    }

    private val cache = Misc.getLRUCache<String, CachedPoint>(1000)

    fun getCachedPoint(id: String,point: NumassPoint): CachedPoint = cache.getOrPut(id) { CachedPoint(point) }

    fun getSpectrumAsync(id: String, point: NumassPoint): Deferred<Table> =
        getCachedPoint(id, point).spectrum

    suspend fun getChannelSpectra(id: String, point: NumassPoint): Map<Int, Table> =
        getCachedPoint(id, point).channelSpectra.await()

    fun clear(){
        cache.clear()
    }
}


//class CachedSet(set: NumassSet, context: Context)  {
//    override val points: ObservableList<CachedPoint> by lazy {
//        set.points.map { CachedPoint(it, context) }.toObservable()
//    }
//    init {
//        var watcher: WatchService? = null
//
//        if (set is NumassDataLoader) {
//            context.launch(Dispatchers.IO) {
//                watcher = set.path.fileSystem.newWatchService()
//                try {
//                    val key: WatchKey = set.path.register(watcher!!, ENTRY_CREATE)
//                    while (true) {
//                        key.pollEvents().forEach { event ->
//                            if (event.kind() == ENTRY_CREATE) {
//                                val path: Path = event.context() as Path
//                                if (path.fileName.toString().startsWith(NumassDataLoader.POINT_FRAGMENT_NAME)) {
//                                    val envelope: Envelope = NumassEnvelopeType.infer(path)?.reader?.read(path)
//                                        ?: kotlin.error("Can't read point file")
//                                    val point = NumassDataUtils.read(envelope)
//                                    points.add(CachedPoint(point, context))
//                                }
//                            }
//                        }
//                    }
//                } catch (x: IOException) {
//                    x.printStackTrace()
//                }
//            }
//        }
//    }
//}
