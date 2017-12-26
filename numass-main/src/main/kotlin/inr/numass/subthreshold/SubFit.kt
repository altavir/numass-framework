package inr.numass.subthreshold

import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataSet
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.pipe
import hep.dataforge.kodex.toList
import hep.dataforge.meta.Meta
import hep.dataforge.storage.commons.StorageUtils
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import hep.dataforge.tables.ValueMap
import hep.dataforge.values.Values
import inr.numass.data.analyzers.NumassAnalyzer.Companion.CHANNEL_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_KEY
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.analyzers.spectrumWithBinning
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassStorageFactory
import org.apache.commons.math3.analysis.ParametricUnivariateFunction
import org.apache.commons.math3.exception.DimensionMismatchException
import org.apache.commons.math3.fitting.SimpleCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoint
import java.util.stream.Collectors

internal fun getSpectraMap(context: Context, meta: Meta): DataNode<Table> {

    //creating storage instance
    val storage = NumassStorageFactory.buildLocal(context, meta.getString("data.dir"), true, false);

    //Reading points
    //Free operation. No reading done
    val sets = StorageUtils
            .loaderStream(storage)
            .filter { it.fullName.toString().matches(meta.getString("data.mask").toRegex()) }
            .map {
                println("loading ${it.fullName}")
                it as NumassSet
            }.collect(Collectors.toList());

    val analyzer = TimeAnalyzer();

    val data = DataSet.builder(NumassPoint::class.java).also { dataBuilder ->
        sets.sortedBy { it.startTime }
                .flatMap { set -> set.points.toList() }
                .groupBy { it.voltage }
                .forEach { key, value ->
                    val point = SimpleNumassPoint(key, value)
                    val name = key.toInt().toString()
                    dataBuilder.putStatic(name, point, buildMeta("meta", "voltage" to key));
                }
    }.build()

    return data.pipe(context, meta) {
        result { input ->
            analyzer.getAmplitudeSpectrum(input, this.meta)
        }
    }
//    val id = buildMeta {
//        +meta.getMeta("data")
//        +meta.getMeta("analyze")
//    }
//    return context.getFeature(CachePlugin::class.java).cacheNode("subThreshold", id, spectra)
}

private val pointNames = arrayOf("U", "amp", "expConst", "correction");

/**
 * Exponential function for fitting
 */
private class ExponentFunction : ParametricUnivariateFunction {
    override fun value(x: Double, vararg parameters: Double): Double {
        if (parameters.size != 2) {
            throw DimensionMismatchException(parameters.size, 2);
        }
        val a = parameters[0];
        val sigma = parameters[1];
        //return a * (Math.exp(x / sigma) - 1);
        return a * Math.exp(x / sigma);
    }

    override fun gradient(x: Double, vararg parameters: Double): DoubleArray {
        if (parameters.size != 2) {
            throw DimensionMismatchException(parameters.size, 2);
        }
        val a = parameters[0];
        val sigma = parameters[1];
        return doubleArrayOf(Math.exp(x / sigma), -a * x / sigma / sigma * Math.exp(x / sigma))
    }

}


/**
 * Calculate underflow exponent parameters using (xLow, xHigh) window for
 * extrapolation
 *
 * @param xLow
 * @param xHigh
 * @return
 */
private fun getUnderflowExpParameters(spectrum: Table, xLow: Int, xHigh: Int, binning: Int): Pair<Double, Double> {
    try {
        if (xHigh <= xLow) {
            throw IllegalArgumentException("Wrong borders for underflow calculation");
        }
        val binned = TableTransform.filter(
                spectrumWithBinning(spectrum, binning),
                CHANNEL_KEY,
                xLow,
                xHigh
        );

        val points = binned.rows
                .map {
                    WeightedObservedPoint(
                            1.0,//1d / p.getValue() , //weight
                            it.getDouble(CHANNEL_KEY), // x
                            it.getDouble(COUNT_RATE_KEY) / binning) //y
                }
                .collect(Collectors.toList());
        val fitter = SimpleCurveFitter.create(ExponentFunction(), doubleArrayOf(1.0, 200.0))
        val res = fitter.fit(points)
        return Pair(res[0], res[1])
    } catch (ex: Exception) {
        return Pair(0.0, 0.0);
    }
}


internal fun fitPoint(voltage: Double, spectrum: Table, xLow: Int, xHigh: Int, upper: Int, binning: Int): Values {
    val norm = spectrum.rows.filter { row ->
        row.getInt(CHANNEL_KEY) in (xLow + 1)..(upper - 1);
    }.mapToDouble { it.getValue(COUNT_RATE_KEY).doubleValue() }.sum();

    val (a, sigma) = getUnderflowExpParameters(spectrum, xLow, xHigh, binning);

    return ValueMap.of(pointNames, voltage, a, sigma, a * sigma * Math.exp(xLow / sigma) / norm + 1.0);
}

internal fun fitAllPoints(data: Map<Double, Table>, xLow: Int, xHigh: Int, upper: Int, binning: Int): Table {
    return ListTable.Builder(*pointNames).apply {
        data.forEach { voltage, spectrum -> row(fitPoint(voltage, spectrum, xLow, xHigh, upper, binning)) }
    }.build()
}