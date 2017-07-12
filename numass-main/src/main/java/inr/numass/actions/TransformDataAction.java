package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Values;
import inr.numass.utils.ExpressionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static hep.dataforge.values.ValueType.NUMBER;
import static hep.dataforge.values.ValueType.STRING;
import static inr.numass.utils.TritiumUtils.pointExpression;

/**
 * Apply corrections and transformations to analyzed data
 * Created by darksnake on 11.07.2017.
 */
public class TransformDataAction extends OneToOneAction<Table, Table> {
    @Override
    protected Table execute(Context context, String name, Table input, Laminate meta) {
        UnaryOperator<Values> transformation = UnaryOperator.identity();





        List<Correction> corrections = new ArrayList<>();
        if (meta.optMeta("correction").isPresent()) {
            corrections.addAll(meta.getMetaList("correction").stream()
                    .map((Function<Meta, Correction>) this::makeCorrection)
                    .collect(Collectors.toList()));
        }

        if (meta.hasValue("correction")) {
            final String correction = meta.getString("correction");
            corrections.add((point) -> pointExpression(correction, point));
        }

        Function<Double, Double> utransform;
        if (meta.hasValue("utransform")) {
            String func = meta.getString("utransform");
            utransform = u -> {
                Map<String, Object> binding = new HashMap<>();
                binding.put("U", u);
                return ExpressionUtils.function(func, binding);
            };
        } else {
            utransform = Function.identity();
        }
    }


    @ValueDef(name = "value", type = {NUMBER, STRING}, info = "Value or function to multiply count rate")
    @ValueDef(name = "err", type = {NUMBER, STRING}, info = "error of the value")
    private Correction makeCorrection(Meta corrMeta) {
        final String expr = corrMeta.getString("value");
        final String errExpr = corrMeta.getString("err", "");
        return new Correction() {
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
        };
    }

    private interface Correction {
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
