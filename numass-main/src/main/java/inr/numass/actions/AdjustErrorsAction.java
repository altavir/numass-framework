/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataPoint;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.ListDataSet;
import hep.dataforge.data.MapDataPoint;

import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.log.Logable;
import hep.dataforge.meta.Meta;
import java.util.ArrayList;
import java.util.List;

/**
 * Adjust errors for all numass points in the dataset
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
@TypedActionDef(name = "adjustErrors", inputType = DataSet.class, outputType = DataSet.class)
public class AdjustErrorsAction extends OneToOneAction<DataSet, DataSet> {

    public AdjustErrorsAction(Context context, Meta annotation) {
        super(context, annotation);
    }

    @Override
    protected DataSet execute(Logable log, Meta meta, DataSet input) {
        List<DataPoint> points = new ArrayList<>();
        for (DataPoint dp : input) {
            points.add(evalPoint(meta, dp));
        }

        return new ListDataSet(input.getName(), input.meta(), points, input.getDataFormat());
    }

    private DataPoint evalPoint(Meta meta, DataPoint dp) {
        if (meta.hasNode("point")) {
            for (Meta pointMeta : meta.getNodes("point")) {
                if (pointMeta.getDouble("Uset") == dp.getDouble("Uset")) {
                    return adjust(dp, pointMeta);
                }
            }
        }

        if (meta.hasNode("range")) {
            for (Meta rangeMeta : meta.getNodes("range")) {
                double from = rangeMeta.getDouble("from", 0);
                double to = rangeMeta.getDouble("to", Double.POSITIVE_INFINITY);
                double u = rangeMeta.getDouble("Uset");
                if (rangeMeta.getDouble("Uset") == dp.getDouble("Uset")) {
                    return adjust(dp, rangeMeta);
                }
            }
        }

        if (meta.hasNode("all")) {
            return adjust(dp, meta.getNode("all"));
        }

        return dp;
    }

    private DataPoint adjust(DataPoint dp, Meta config) {
        MapDataPoint res = new MapDataPoint(dp);
        if (res.hasValue("CRerr")) {
            double instability = 0;
            if (dp.hasValue("CR")) {
                instability = dp.getDouble("CR") * config.getDouble("instability", 0);
            }

            double factor = config.getDouble("factor", 1d);
            double base = config.getDouble("base", 0);
            double adjusted = res.getDouble("CRerr") * factor + instability + base;
            res.putValue("CRerr", adjusted);
        } else {
            throw new RuntimeException("The value CRerr is not found in the data point!");
        }
        return res;
    }
}
