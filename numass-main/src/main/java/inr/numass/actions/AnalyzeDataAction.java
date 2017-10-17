package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.tables.Table;
import inr.numass.data.analyzers.SmartAnalyzer;
import inr.numass.data.api.NumassAnalyzer;
import inr.numass.data.api.NumassSet;
import inr.numass.utils.NumassUtils;

import static hep.dataforge.values.ValueType.NUMBER;
import static hep.dataforge.values.ValueType.STRING;

/**
 * The action performs the readout of data and collection of count rate into a table
 * Created by darksnake on 11.07.2017.
 */
@TypedActionDef(name = "numass.analyze", inputType = NumassSet.class, outputType = Table.class)
@ValueDef(name = "window.lo", type = {NUMBER, STRING}, def = "0", info = "Lower bound for window")
@ValueDef(name = "window.up", type = {NUMBER, STRING}, def = "10000", info = "Upper bound for window")
public class AnalyzeDataAction extends OneToOneAction<NumassSet, Table> {
    @Override
    protected Table execute(Context context, String name, NumassSet input, Laminate inputMeta) {
        //TODO add processor here
        NumassAnalyzer analyzer = new SmartAnalyzer();
        Table res = analyzer.analyzeSet(input, inputMeta);
        output(context, name, stream -> NumassUtils.writeSomething(stream, inputMeta, res));
        return res;
    }
}
