package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.names.Named;
import hep.dataforge.tables.ColumnFormat;
import hep.dataforge.tables.ColumnTable;
import hep.dataforge.tables.ListColumn;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Values;
import inr.numass.utils.NumassUtils;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

import static hep.dataforge.values.ValueType.NUMBER;
import static hep.dataforge.values.ValueType.STRING;
import static inr.numass.data.api.NumassAnalyzer.COUNT_RATE_ERROR_KEY;
import static inr.numass.data.api.NumassAnalyzer.COUNT_RATE_KEY;
import static inr.numass.utils.NumassUtils.pointExpression;

/**
 * Apply corrections and transformations to analyzed data
 * Created by darksnake on 11.07.2017.
 */
@TypedActionDef(name = "numass.transform", inputType = Table.class, outputType = Table.class)
@ValueDef(name = "correction",
        info = "An expression to correct count number depending on potential `U`, point length `T` and point itself as `point`")
@ValueDef(name = "utransform", info = "Expression for voltage transformation. Uses U as input")
@NodeDef(name = "correction", multiple = true, from = "method::inr.numass.actions.TransformDataAction.makeCorrection")
public class TransformDataAction extends OneToOneAction<Table, Table> {

    @Override
    protected Table execute(Context context, String name, Table input, Laminate meta) {

        List<Correction> corrections = new ArrayList<>();

        meta.optMeta("corrections").ifPresent(cors ->
                MetaUtils.nodeStream(cors)
                        .map(Pair::getValue)
                        .map(this::makeCorrection)
                        .forEach(corrections::add)
        );

        if (meta.hasValue("correction")) {
            final String correction = meta.getString("correction");
            corrections.add(point -> pointExpression(correction, point));
        }


        ColumnTable table = ColumnTable.copy(input);

        for (Correction correction : corrections) {
            //adding correction columns
            if (!correction.isAnonimous()) {
                table = table.buildColumn(ColumnFormat.build(correction.getName(), NUMBER),
                        correction::corr);
                if (correction.hasError()) {
                    table = table.buildColumn(ColumnFormat.build(correction.getName() + ".err", NUMBER),
                            correction::corrErr);
                }
            }
        }

        // adding original count rate and error columns
        table = table.addColumn(new ListColumn(ColumnFormat.build(COUNT_RATE_KEY + ".orig", NUMBER), table.getColumn
                (COUNT_RATE_KEY).stream()));
        table = table.addColumn(new ListColumn(ColumnFormat.build(COUNT_RATE_ERROR_KEY + ".orig", NUMBER), table
                .getColumn(COUNT_RATE_ERROR_KEY).stream()));

        List<Double> cr = new ArrayList<>();
        List<Double> crErr = new ArrayList<>();

        table.getRows().forEach(point -> {
            double correctionFactor = corrections.stream()
                    .mapToDouble(cor -> cor.corr(point))
                    .reduce((d1, d2) -> d1 * d2).orElse(1);
            double relativeCorrectionError = Math.sqrt(
                    corrections.stream()
                            .mapToDouble(cor -> cor.relativeErr(point))
                            .reduce((d1, d2) -> d1 * d1 + d2 * d2).orElse(0)
            );
            double originalCR = point.getDouble(COUNT_RATE_KEY);
            double originalCRErr = point.getDouble(COUNT_RATE_ERROR_KEY);
            cr.add(originalCR * correctionFactor);
            if (relativeCorrectionError == 0) {
                crErr.add(originalCRErr * correctionFactor);
            } else {
                crErr.add(Math.sqrt(Math.pow(originalCRErr / originalCR, 2d) + Math.pow(relativeCorrectionError, 2d))
                        * originalCR);
            }
        });

        //replacing cr column
        Table res = table.addColumn(ListColumn.build(table.getColumn(COUNT_RATE_KEY).getFormat(), cr.stream()))
                .addColumn(ListColumn.build(table.getColumn(COUNT_RATE_ERROR_KEY).getFormat(), crErr.stream()));

        output(context, name, stream -> NumassUtils.writeSomething(stream, meta, res));
        return res;
    }


    @ValueDef(name = "value", type = {NUMBER, STRING}, info = "Value or function to multiply count rate")
    @ValueDef(name = "err", type = {NUMBER, STRING}, info = "error of the value")
    private Correction makeCorrection(Meta corrMeta) {
        final String expr = corrMeta.getString("value");
        final String errExpr = corrMeta.getString("err", "");
        return new Correction() {
            @Override
            public String getName() {
                return corrMeta.getString("name", corrMeta.getName());
            }

            @Override
            public double corr(Values point) {
                return pointExpression(expr, point);
            }

            @Override
            public double corrErr(Values point) {
                if (errExpr.isEmpty()) {
                    return 0;
                } else {
                    return pointExpression(errExpr, point);
                }
            }

            @Override
            public boolean hasError() {
                return !errExpr.isEmpty();
            }
        };
    }

    private interface Correction extends Named {

        @Override
        default String getName() {
            return "";
        }

        /**
         * correction coefficient
         *
         * @param point
         * @return
         */
        double corr(Values point);

        /**
         * correction coefficient uncertainty
         *
         * @param point
         * @return
         */
        default double corrErr(Values point) {
            return 0;
        }

        default boolean hasError() {
            return false;
        }

        default double relativeErr(Values point) {
            double corrErr = corrErr(point);
            if (corrErr == 0) {
                return 0;
            } else {
                return corrErr / corr(point);
            }
        }
    }

}
