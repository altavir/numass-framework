package inr.numass.scripts.underflow

import hep.dataforge.cache.CachePlugin
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataSet
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.workspace.GrindPipe
import hep.dataforge.meta.Meta
import hep.dataforge.storage.commons.StorageUtils
import hep.dataforge.tables.Table
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassStorage
import inr.numass.data.storage.NumassStorageFactory

import java.util.stream.Collectors

import static hep.dataforge.grind.Grind.buildMeta

class UnderflowUtils {

    static DataNode<Table> getSpectraMap(GrindShell shell, Meta meta) {
        return shell.eval {
            //Defining root directory
            File dataDirectory = new File(meta.getString("data.dir"))

            //creating storage instance

            NumassStorage storage = NumassStorageFactory.buildLocal(dataDirectory);

            //Reading points
            //Free operation. No reading done
            List<NumassSet> sets = StorageUtils
                    .loaderStream(storage)
                    .filter { it.key.matches(meta.getString("data.mask")) }
                    .map {
                println "loading ${it.key}"
                return it.value
            }.collect(Collectors.toList());

            NumassAnalyzer analyzer = new TimeAnalyzer();

            def dataBuilder = DataSet.builder(NumassPoint);

            sets.sort { it.startTime }
                .collectMany {NumassSet set -> set.points.collect() }
                .groupBy { NumassPoint point -> point.voltage }
                .each { key, value ->
                    def point = new SimpleNumassPoint(key as double, value as List<NumassPoint>)
                    String name = (key as Integer).toString()
                    dataBuilder.putStatic(name, point, buildMeta(voltage: key));
                }

            DataNode<NumassPoint> data = dataBuilder.build()

            def generate = GrindPipe.<NumassPoint, Table> build(name: "generate") {
                return analyzer.getSpectrum(delegate.input as NumassPoint, delegate.meta)
            }

            DataNode<Table> spectra = generate.run(shell.context, data, meta.getMeta("generate"));
            Meta id = buildMeta {
                put meta.getMeta("data")
                put meta.getMeta("generate")
            }
            return shell.context.getFeature(CachePlugin).cacheNode("underflow", id, spectra)
        } as DataNode<Table>
    }
}
