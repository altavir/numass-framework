package inr.numass.actions

import hep.dataforge.actions.OneToOneAction
import hep.dataforge.context.Context
import hep.dataforge.description.TypedActionDef
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.Laminate
import hep.dataforge.tables.Table
import hep.dataforge.values.ValueType.NUMBER
import hep.dataforge.values.ValueType.STRING
import inr.numass.NumassUtils
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.api.NumassSet
import inr.numass.data.analyzers.SmartAnalyzer

/**
 * The action performs the readout of data and collection of count rate into a table
 * Created by darksnake on 11.07.2017.
 */
@TypedActionDef(name = "numass.analyze", inputType = NumassSet::class, outputType = Table::class)
@ValueDefs(
        ValueDef(key = "window.lo", type = arrayOf(NUMBER, STRING), def = "0", info = "Lower bound for window"),
        ValueDef(key = "window.up", type = arrayOf(NUMBER, STRING), def = "10000", info = "Upper bound for window")
)
object AnalyzeDataAction : OneToOneAction<NumassSet, Table>("numass.analyze", NumassSet::class.java, Table::class.java) {
    override fun execute(context: Context, name: String, input: NumassSet, inputMeta: Laminate): Table {
        //TODO add processor here
        val analyzer: NumassAnalyzer = SmartAnalyzer()
        val res = analyzer.analyzeSet(input, inputMeta)

        render(context, name, NumassUtils.wrap(res, inputMeta))
//        output(context, name) { stream -> NumassUtils.write(stream, inputMeta, res) }
        return res
    }
}
