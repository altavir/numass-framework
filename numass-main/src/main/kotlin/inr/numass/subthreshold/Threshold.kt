package inr.numass.subthreshold

import hep.dataforge.actions.pipe
import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.nullable
import hep.dataforge.storage.Storage
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.values.ValueMap
import hep.dataforge.values.Values
import inr.numass.data.analyzers.*
import inr.numass.data.analyzers.NumassAnalyzer.Companion.CHANNEL_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_KEY
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassDataLoader
import inr.numass.data.storage.NumassDirectory
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.analysis.ParametricUnivariateFunction
import org.apache.commons.math3.exception.DimensionMismatchException
import org.apache.commons.math3.fitting.SimpleCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoint
import org.slf4j.LoggerFactory
import java.util.stream.Collectors
import java.util.stream.StreamSupport
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow


object Threshold {

    suspend fun getSpectraMap(context: Context, meta: Meta): DataNode<Table> {

        //creating storage instance
        val storage = NumassDirectory.read(context, meta.getString("data.dir")) as Storage

        fun Storage.loaders(): Sequence<NumassDataLoader> {
            return sequence<NumassDataLoader> {
                print("Reading ${this@loaders.fullName}")
                runBlocking { this@loaders.getChildren() }.forEach {
                    if (it is NumassDataLoader) {
                        yield(it)
                    } else if (it is Storage) {
                        yieldAll(it.loaders())
                    }
                }
            }
        }

        //Reading points
        //Free operation. No reading done
        val sets = storage.loaders()
            .filter { it.fullName.toString().matches(meta.getString("data.mask").toRegex()) }

        val analyzer = TimeAnalyzer();

        val data = DataSet.edit(NumassPoint::class).also { dataBuilder ->
            sets.sortedBy { it.startTime }
                .flatMap { set -> set.points.asSequence() }
                .groupBy { it.voltage }
                .forEach { key, value ->
                    val point = SimpleNumassPoint.build(value, key)
                    val name = key.toInt().toString()
                    dataBuilder.putStatic(name, point, buildMeta("meta", "voltage" to key));
                }
        }.build()

        return data.pipe(context, meta) {
            result { input ->
                analyzer.getAmplitudeSpectrum(input, this.meta)
            }
        }
    }


    /**
     * Prepare the list of weighted points for fitter
     */
    private fun preparePoints(spectrum: Table, xLow: Int, xHigh: Int, binning: Int): List<WeightedObservedPoint> {
        if (xHigh <= xLow) {
            throw IllegalArgumentException("Wrong borders for underflow calculation");
        }
        val binned = spectrum.withBinning(binning, xLow, xHigh)
//        val binned = TableTransform.filter(
//                spectrum.withBinning(binning),
//                CHANNEL_KEY,
//                xLow,
//                xHigh
//        )

        return binned.rows
            .map {
                WeightedObservedPoint(
                    1.0,//1d / p.getValue() , //weight
                    it.getDouble(CHANNEL_KEY), // x
                    it.getDouble(COUNT_RATE_KEY) / binning
                ) //y
            }
            .collect(Collectors.toList())
    }

    private fun norm(spectrum: Table, xLow: Int, upper: Int): Double {
        return spectrum.rows.filter { row ->
            row.getInt(CHANNEL_KEY) in (xLow + 1)..(upper - 1)
        }.mapToDouble { it.getValue(COUNT_RATE_KEY).double }.sum()
    }

    private val expPointNames = arrayOf("U", "amp", "expConst", "correction");

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
            return a * exp(x / sigma);
        }

        override fun gradient(x: Double, vararg parameters: Double): DoubleArray {
            if (parameters.size != 2) {
                throw DimensionMismatchException(parameters.size, 2);
            }
            val a = parameters[0];
            val sigma = parameters[1];
            return doubleArrayOf(exp(x / sigma), -a * x / sigma / sigma * exp(x / sigma))
        }
    }


    /**
     * Exponential function $a e^{\frac{x}{\sigma}}$
     */
    private fun exponential(point: NumassPoint, spectrum: Table, config: Meta): Values {
        val xLow: Int = config.getInt("xLow", 400)
        val xHigh: Int = config.getInt("xHigh", 700)
        val upper: Int = config.getInt("upper", 3100)
        val binning: Int = config.getInt("binning", 20)


        val fitter = SimpleCurveFitter.create(ExponentFunction(), doubleArrayOf(1.0, 200.0))
        val (a, sigma) = fitter.fit(preparePoints(spectrum, xLow, xHigh, binning))

        val norm = norm(spectrum, xLow, upper)

        return ValueMap.ofPairs(
            "index" to point.index,
            "U" to point.voltage,
            "a" to a,
            "sigma" to sigma,
            "correction" to a * sigma * exp(xLow / sigma) / norm + 1.0
        )
    }


    private class PowerFunction(val shift: Double? = null) : ParametricUnivariateFunction {

        override fun value(x: Double, vararg parameters: Double): Double {
            val a = parameters[0]
            val beta = parameters[1]
            val delta = shift ?: parameters[2]

            return a * (x - delta).pow(beta)
        }

        override fun gradient(x: Double, vararg parameters: Double): DoubleArray {
            val a = parameters[0]
            val beta = parameters[1]
            val delta = shift ?: parameters[2]
            return if (parameters.size > 2) {
                doubleArrayOf(
                    (x - delta).pow(beta),
                    a * (x - delta).pow(beta) * ln(x - delta),
                    -a * beta * (x - delta).pow(beta - 1)
                )
            } else {
                doubleArrayOf(
                    (x - delta).pow(beta),
                    a * (x - delta).pow(beta) * ln(x - delta)
                )
            }
        }
    }

    /**
     * Power function $a (x-\delta)^{\beta}
     */
    private fun power(point: NumassPoint, spectrum: Table, config: Meta): Values {
        val xLow: Int = config.getInt("xLow", 400)
        val xHigh: Int = config.getInt("xHigh", 700)
        val upper: Int = config.getInt("upper", 3100)
        val binning: Int = config.getInt("binning", 20)

        //val fitter = SimpleCurveFitter.create(PowerFunction(), doubleArrayOf(1e-2, 1.5,0.0))
        //val (a, beta, delta) = fitter.fit(preparePoints(spectrum, xLow, xHigh, binning))

        val delta = config.getDouble("delta", 0.0)
        val fitter = SimpleCurveFitter.create(PowerFunction(delta), doubleArrayOf(1e-2, 1.5))
        val (a, beta) = fitter.fit(preparePoints(spectrum, xLow, xHigh, binning))


        val norm = norm(spectrum, xLow, upper)

        return ValueMap.ofPairs(
            "index" to point.index,
            "U" to point.voltage,
            "a" to a,
            "beta" to beta,
            "delta" to delta,
            "correction" to a / (beta + 1) * (xLow - delta).pow(beta + 1.0) / norm + 1.0
        )
    }

    fun calculateSubThreshold(point: NumassPoint, spectrum: Table, config: Meta): Values {
        return when (config.getString("method", "exp")) {
            "exp" -> exponential(point, spectrum, config)
            "pow" -> power(point, spectrum, config)
            else -> throw RuntimeException("Unknown sub threshold calculation method")
        }
    }

    fun calculateSubThreshold(set: NumassSet, config: Meta, analyzer: NumassAnalyzer = SmartAnalyzer()): Table {
        val reference = config.optNumber("reference").nullable?.let {
            set.getPoints(it.toDouble()).firstOrNull() ?: error("Reference point not found")
        }?.let {
            println("Using reference point ${it.voltage}")
            analyzer.getAmplitudeSpectrum(it, config)
        }

        return ListTable.Builder().apply {
            StreamSupport.stream(set.spliterator(), true).map { point ->
                LoggerFactory.getLogger(Threshold.javaClass).info("Starting point ${point.voltage}")
                val spectrum = analyzer.getAmplitudeSpectrum(point, config).let {
                    if (reference == null) {
                        it
                    } else {
                        subtractAmplitudeSpectrum(it, reference)
                    }
                }
                LoggerFactory.getLogger(Threshold.javaClass).info("Calculating threshold ${point.voltage}")
                return@map try {
                    calculateSubThreshold(point, spectrum, config)
                } catch (ex: Exception) {
                    LoggerFactory.getLogger(Threshold.javaClass).error("Failed to fit point ${point.voltage}", ex)
                    null
                }
            }.toList().filterNotNull().forEach {
                println(it.toString())
                row(it)
            }
        }.build()
    }

}