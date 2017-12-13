package inr.numass.actions

import hep.dataforge.actions.OneToOneAction
import hep.dataforge.context.Context
import hep.dataforge.description.NodeDef
import hep.dataforge.description.TypedActionDef
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import hep.dataforge.names.Named
import hep.dataforge.tables.ColumnFormat
import hep.dataforge.tables.ColumnTable
import hep.dataforge.tables.ListColumn
import hep.dataforge.tables.Table
import hep.dataforge.values.ValueType.NUMBER
import hep.dataforge.values.ValueType.STRING
import hep.dataforge.values.Values
import inr.numass.NumassUtils
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_ERROR_KEY
import inr.numass.data.analyzers.NumassAnalyzer.Companion.COUNT_RATE_KEY
import inr.numass.pointExpression
import java.util.*

/**
 * Apply corrections and transformations to analyzed data
 * Created by darksnake on 11.07.2017.
 */
@TypedActionDef(name = "numass.transform", inputType = Table::class, outputType = Table::class)
@ValueDefs(
        ValueDef(name = "correction", info = "An expression to correct count number depending on potential `U`, point length `T` and point itself as `point`"),
        ValueDef(name = "utransform", info = "Expression for voltage transformation. Uses U as input")
)
@NodeDef(name = "correction", multiple = true, from = "method::inr.numass.actions.TransformDataAction.makeCorrection")
class TransformDataAction : OneToOneAction<Table, Table>() {

    override fun execute(context: Context, name: String, input: Table, meta: Laminate): Table {

        val corrections = ArrayList<Correction>()

        meta.optMeta("corrections").ifPresent { cors ->
            MetaUtils.nodeStream(cors)
                    .map<Meta> { it.value }
                    .map<Correction> { this.makeCorrection(it) }
                    .forEach { corrections.add(it) }
        }

        if (meta.hasValue("correction")) {
            val correction = meta.getString("correction")
            corrections.add(object : Correction {
                override fun corr(point: Values): Double {
                    return pointExpression(correction, point)
                }
            })
        }


        var table = ColumnTable.copy(input)

        for (correction in corrections) {
            //adding correction columns
            if (!correction.isAnonimous) {
                table = table.buildColumn(ColumnFormat.build(correction.name, NUMBER)) { correction.corr(it) }
                if (correction.hasError()) {
                    table = table.buildColumn(ColumnFormat.build(correction.name + ".err", NUMBER)) { correction.corrErr(it) }
                }
            }
        }

        // adding original count rate and error columns
        table = table.addColumn(ListColumn(ColumnFormat.build(COUNT_RATE_KEY + ".orig", NUMBER), table.getColumn(COUNT_RATE_KEY).stream()))
        table = table.addColumn(ListColumn(ColumnFormat.build(COUNT_RATE_ERROR_KEY + ".orig", NUMBER), table
                .getColumn(COUNT_RATE_ERROR_KEY).stream()))

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
            val originalCR = point.getDouble(COUNT_RATE_KEY)!!
            val originalCRErr = point.getDouble(COUNT_RATE_ERROR_KEY)!!
            cr.add(originalCR * correctionFactor)
            if (relativeCorrectionError == 0.0) {
                crErr.add(originalCRErr * correctionFactor)
            } else {
                crErr.add(Math.sqrt(Math.pow(originalCRErr / originalCR, 2.0) + Math.pow(relativeCorrectionError, 2.0)) * originalCR)
            }
        }

        //replacing cr column
        val res = table.addColumn(ListColumn.build(table.getColumn(COUNT_RATE_KEY).format, cr.stream()))
                .addColumn(ListColumn.build(table.getColumn(COUNT_RATE_ERROR_KEY).format, crErr.stream()))

        output(context, name) { stream -> NumassUtils.write(stream, meta, res) }
        return res
    }


    @ValueDefs(
            ValueDef(name = "value", type = arrayOf(NUMBER, STRING), info = "Value or function to multiply count rate"),
            ValueDef(name = "err", type = arrayOf(NUMBER, STRING), info = "error of the value")
    )
    private fun makeCorrection(corrMeta: Meta): Correction {
        val expr = corrMeta.getString("value")
        val errExpr = corrMeta.getString("err", "")
        return object : Correction {
            override fun getName(): String {
                return corrMeta.getString("name", corrMeta.name)
            }

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
                return !errExpr.isEmpty()
            }
        }
    }

    private interface Correction : Named {

        override fun getName(): String {
            return ""
        }

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

}
