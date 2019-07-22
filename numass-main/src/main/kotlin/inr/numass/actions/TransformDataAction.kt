package inr.numass.actions

import hep.dataforge.Named
import hep.dataforge.actions.OneToOneAction
import hep.dataforge.context.Context
import hep.dataforge.description.NodeDef
import hep.dataforge.description.TypedActionDef
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.isAnonymous
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import hep.dataforge.tables.ColumnFormat
import hep.dataforge.tables.ColumnTable
import hep.dataforge.tables.ListColumn
import hep.dataforge.tables.Table
import hep.dataforge.values.ValueType.NUMBER
import hep.dataforge.values.ValueType.STRING
import hep.dataforge.values.Values
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_ERROR_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_KEY
import inr.numass.pointExpression
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Apply corrections and transformations to analyzed data
 * Created by darksnake on 11.07.2017.
 */
@TypedActionDef(name = "numass.transform", inputType = Table::class, outputType = Table::class)
@ValueDefs(
    ValueDef(
        key = "correction",
        info = "An expression to correct count number depending on potential `U`, point length `T` and point itself as `point`"
    ),
    ValueDef(key = "utransform", info = "Expression for voltage transformation. Uses U as input")
)
@NodeDef(
    key = "correction",
    multiple = true,
    descriptor = "method::inr.numass.actions.TransformDataAction.makeCorrection"
)
object TransformDataAction : OneToOneAction<Table, Table>("numass.transform", Table::class.java, Table::class.java) {

    override fun execute(context: Context, name: String, input: Table, meta: Laminate): Table {

        var table = ColumnTable.copy(input)

        val corrections = ArrayList<Correction>()

        meta.optMeta("corrections").ifPresent { cors ->
            MetaUtils.nodeStream(cors)
                .filter { it.first.length == 1 }
                .map<Meta> { it.second }
                .map { makeCorrection(it) }
                .forEach { corrections.add(it) }
        }

        if (meta.hasValue("correction")) {
            val correction = meta.getString("correction")
            corrections.add(object : Correction {
                override val name: String = ""

                override fun corr(point: Values): Double {
                    return pointExpression(correction, point)
                }
            })
        }

        for (correction in corrections) {
            //adding correction columns
            if (!correction.isAnonymous) {
                table = table.buildColumn(ColumnFormat.build(correction.name, NUMBER)) { correction.corr(this) }
                if (correction.hasError()) {
                    table = table.buildColumn(ColumnFormat.build(correction.name + ".err", NUMBER)) {
                        correction.corrErr(this)
                    }
                }
            }
        }


        // adding original count rate and error columns
        table = table.addColumn(
            ListColumn(
                ColumnFormat.build("$COUNT_RATE_KEY.orig", NUMBER),
                table.getColumn(COUNT_RATE_KEY).stream()
            )
        )
        table = table.addColumn(
            ListColumn(
                ColumnFormat.build("$COUNT_RATE_ERROR_KEY.orig", NUMBER), table
                    .getColumn(COUNT_RATE_ERROR_KEY).stream()
            )
        )

        val cr = ArrayList<Double>()
        val crErr = ArrayList<Double>()

        table.rows.forEach { point ->
            val correctionFactor = corrections.stream()
                .mapToDouble { cor -> cor.corr(point) }
                .reduce { d1, d2 -> d1 * d2 }.orElse(1.0)
            val relativeCorrectionError = Math.sqrt(
                corrections.stream()
                    .mapToDouble { cor -> cor.relativeErr(point) }
                    .reduce { d1, d2 -> d1 * d1 + d2 * d2 }.orElse(0.0)
            )
            val originalCR = point.getDouble(COUNT_RATE_KEY)
            val originalCRErr = point.getDouble(COUNT_RATE_ERROR_KEY)
            cr.add(originalCR * correctionFactor)
            if (relativeCorrectionError == 0.0) {
                crErr.add(originalCRErr * correctionFactor)
            } else {
                crErr.add(sqrt((originalCRErr / originalCR).pow(2.0) + relativeCorrectionError.pow(2.0)) * originalCR)
            }
        }

        //replacing cr column
        val res = table.addColumn(ListColumn.build(table.getColumn(COUNT_RATE_KEY).format, cr.stream()))
            .addColumn(ListColumn.build(table.getColumn(COUNT_RATE_ERROR_KEY).format, crErr.stream()))

        context.output[this@TransformDataAction.name, name].render(res, meta)
        return res
    }


    @ValueDefs(
        ValueDef(key = "value", type = arrayOf(NUMBER, STRING), info = "Value or function to multiply count rate"),
        ValueDef(key = "err", type = arrayOf(NUMBER, STRING), info = "error of the value")
    )
    private fun makeCorrection(corrMeta: Meta): Correction {
        val name = corrMeta.getString("name", corrMeta.name)
        return if (corrMeta.hasMeta("table")) {
            val x = corrMeta.getValue("table.u").list.map { it.double }
            val corr = corrMeta.getValue("table.corr").list.map { it.double }
            TableCorrection(name, x, corr)
        } else {
            val expr = corrMeta.getString("value")
            val errExpr = corrMeta.getString("err", "")
            ExpressionCorrection(name, expr, errExpr)
        }
    }

    interface Correction : Named {

        /**
         * correction coefficient
         *
         * @param point
         * @return
         */
        fun corr(point: Values): Double

        /**
         * correction coefficient uncertainty
         *
         * @param point
         * @return
         */
        fun corrErr(point: Values): Double {
            return 0.0
        }

        fun hasError(): Boolean {
            return false
        }

        fun relativeErr(point: Values): Double {
            val corrErr = corrErr(point)
            return if (corrErr == 0.0) {
                0.0
            } else {
                corrErr / corr(point)
            }
        }
    }

    class ExpressionCorrection(override val name: String, val expr: String, val errExpr: String) : Correction {
        override fun corr(point: Values): Double {
            return pointExpression(expr, point)
        }

        override fun corrErr(point: Values): Double {
            return if (errExpr.isEmpty()) {
                0.0
            } else {
                pointExpression(errExpr, point)
            }
        }

        override fun hasError(): Boolean {
            return errExpr.isNotEmpty()
        }
    }

    class TableCorrection(
        override val name: String,
        val x: List<Double>,
        val y: List<Double>,
        val yErr: List<Double>? = null
    ) : Correction {
        override fun corr(point: Values): Double {
            val voltage = point.getDouble("voltage")
            val index = x.indexOfFirst { it > voltage }
            //TODO add interpolation
            return if (index < 0) {
                y.last()
            } else {
                y[index]
            }
        }
//
//        override fun corrErr(point: Values): Double {
//            return super.corrErr(point)
//        }
//
//        override fun hasError(): Boolean = yErr.isNullOrEmpty()
    }

}
