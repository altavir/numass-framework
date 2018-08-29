/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.values.ValueMap;
import hep.dataforge.values.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * Adjust errors for all numass points in the dataset
 *
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
@TypedActionDef(name = "adjustErrors", inputType = Table.class, outputType = Table.class)
public class AdjustErrorsAction extends OneToOneAction<Table, Table> {

    public AdjustErrorsAction() {
        super("adjustErrors", Table.class, Table.class);
    }

    @Override
    protected Table execute(Context context, String name, Table input, Laminate meta) {
        List<Values> points = new ArrayList<>();
        for (Values dp : input) {
            points.add(evalPoint(meta, dp));
        }

        return new ListTable(input.getFormat(), points);
    }

    private Values evalPoint(Meta meta, Values dp) {
        if (meta.hasMeta("point")) {
            for (Meta pointMeta : meta.getMetaList("point")) {
                if (pointMeta.getDouble("Uset") == dp.getDouble("Uset")) {
                    return adjust(dp, pointMeta);
                }
            }
        }

        if (meta.hasMeta("range")) {
            for (Meta rangeMeta : meta.getMetaList("range")) {
                double from = rangeMeta.getDouble("from", 0);
                double to = rangeMeta.getDouble("to", Double.POSITIVE_INFINITY);
                double u = rangeMeta.getDouble("Uset");
                if (rangeMeta.getDouble("Uset") == dp.getDouble("Uset")) {
                    return adjust(dp, rangeMeta);
                }
            }
        }

        if (meta.hasMeta("all")) {
            return adjust(dp, meta.getMeta("all"));
        }

        return dp;
    }

    private Values adjust(Values dp, Meta config) {
        ValueMap.Builder res = new ValueMap.Builder(dp);
        if (dp.hasValue("CRerr")) {
            double instability = 0;
            if (dp.hasValue("CR")) {
                instability = dp.getDouble("CR") * config.getDouble("instability", 0);
            }

            double factor = config.getDouble("factor", 1d);
            double base = config.getDouble("base", 0);
            double adjusted = dp.getDouble("CRerr") * factor + instability + base;
            res.putValue("CRerr", adjusted);
        } else {
            throw new RuntimeException("The value CRerr is not found in the data point!");
        }
        return res.build();
    }
}
