package inr.numass.viewer

import hep.dataforge.context.ContextAware
import hep.dataforge.meta.Meta
import hep.dataforge.storage.tables.TableLoader
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.utils.Misc
import hep.dataforge.values.ValueMap
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDataLoader
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import kotlinx.coroutines.*
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.attribute.BasicFileAttributes
import kotlin.math.floor

class DataController : Controller(), ContextAware {
    override val context get() = app.context

    val analyzer = TimeAnalyzer()

    inner class CachedPoint(point: NumassPoint) {
        val length = point.length

        val voltage = point.voltage

        val index = point.index

        val meta = point.meta

        val channelSpectra: Deferred<Map<Int, Table>> = context.async {
            point.channels.mapValues { (_, value) -> analyzer.getAmplitudeSpectrum(value) }
        }

        val spectrum: Deferred<Table> = context.async{
            analyzer.getAmplitudeSpectrum(point)
        }

        val timeSpectrum: Deferred<Table> = context.async{
            val cr = spectrum.await().sumOf {
                it.getValue(NumassAnalyzer.COUNT_KEY).int
            }.toDouble() / point.length.toMillis() * 1000

            val binNum = 200
            //inputMeta.getInt("binNum", 1000);
            val binSize = 1.0 / cr * 10 / binNum * 1e6
            //inputMeta.getDouble("binSize", 1.0 / cr * 10 / binNum * 1e6)

            val format = TableFormatBuilder()
                .addNumber("x", Adapters.X_VALUE_KEY)
                .addNumber(NumassAnalyzer.COUNT_KEY, Adapters.Y_VALUE_KEY)
                .build()

            ListTable.Builder(format).rows(
                analyzer.getEventsWithDelay(point, Meta.empty())
                    .map { it.second.toDouble() / 1000.0 }
                    .groupBy { floor(it / binSize) }
                    .toSortedMap()
                    .map {
                        ValueMap.ofPairs("x" to it.key, "count" to it.value.count())
                    }
            ).build()
        }
    }

    private val cache = Misc.getLRUCache<String, CachedPoint>(400)

    fun getCachedPoint(id: String, point: NumassPoint): CachedPoint = cache.getOrPut(id) { CachedPoint(point) }

    fun getSpectrumAsync(id: String, point: NumassPoint): Deferred<Table> =
        getCachedPoint(id, point).spectrum

    suspend fun getChannelSpectra(id: String, point: NumassPoint): Map<Int, Table> =
        getCachedPoint(id, point).channelSpectra.await()

    val sets: ObservableMap<String, NumassSet> = FXCollections.observableHashMap()
    val points: ObservableMap<String, CachedPoint> = FXCollections.observableHashMap()
    val sc: ObservableMap<String, TableLoader> = FXCollections.observableHashMap()

    val files: ObservableList<Path> = FXCollections.observableArrayList()

    val watchPathProperty = SimpleObjectProperty<Path?>()

    private var watchJob: Job? = null

    init {
        watchPathProperty.onChange { watchPath ->
            watchJob?.cancel()
            if (watchPath != null) {
                Files.list(watchPath).toList()
                    .filter {
                        !Files.isDirectory(it) && it.fileName.toString().startsWith(NumassDataLoader.POINT_FRAGMENT_NAME)
                    }
                    .sortedBy { file ->
                        val attr = Files.readAttributes(file, BasicFileAttributes::class.java)
                        attr.creationTime()
                    }.forEach { path ->
                        try {
                            runLater {
                                files.add(path)
                            }
                        } catch (x: Throwable) {
                            app.context.logger.error("Error during dynamic point read", x)
                        }
                    }
                val watcher = watchPath.fileSystem.newWatchService()
                watchJob = app.context.launch {
                    watcher.use { watcher ->
                        val key: WatchKey = watchPath.register(watcher,
                            StandardWatchEventKinds.ENTRY_CREATE)
                        coroutineContext[Job]?.invokeOnCompletion {
                            key.cancel()
                        }
                        while (isActive) {
                            try {
                                key.pollEvents().forEach { event ->
                                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                        val path: Path = event.context() as Path
                                        runLater {
                                            files.add(watchPath.resolve(path))
                                        }
                                    }
                                }
                            } catch (x: Throwable) {
                                app.context.logger.error("Error during dynamic point read", x)
                            }
                        }
                    }
                }
            }
        }
    }

    fun clear() {
        cache.clear()
        sets.clear()
        points.clear()
        sc.clear()
        watchPathProperty.set(null)
    }


    fun addPoint(id: String, point: NumassPoint): CachedPoint {
        val newPoint = getCachedPoint(id, point)
        points[id] = newPoint
        return newPoint
    }

    fun addSet(id: String, set: NumassSet) {
        sets[id] = set
    }

    fun addSc(id: String, set: TableLoader) {
        sc[id] = set
    }

    fun remove(id: String) {
        points.remove(id)
        sets.remove(id)
        sc.remove(id)
    }

//
//    fun addAllPoints(points: Map<String, NumassPoint>) {
//        TODO()
//    }


}