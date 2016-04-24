/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.actions;

import hep.dataforge.actions.GroupBuilder;
import hep.dataforge.actions.ManyToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.log.Logable;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.points.DataPoint;
import hep.dataforge.points.ListPointSet;
import hep.dataforge.points.MapPoint;
import hep.dataforge.points.PointSet;
import hep.dataforge.points.PointSource;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "merge", inputType = PointSet.class, outputType = PointSet.class, description = "Merge different numass data files into one.")
@NodeDef(name = "grouping", info = "The defenition of grouping rule for this merge", target = "method::hep.dataforge.actions.GroupBuilder.byAnnotation")
public class MergeDataAction extends ManyToOneAction<PointSet, PointSet> {

    public static final String MERGE_NAME = "mergeName";
    public static String[] parnames = {"Uset", "Uread", "Length", "Total", "Window", "Corrected", "CR", "CRerr"};

    @Override
    @SuppressWarnings("unchecked")
    protected List<DataNode<PointSet>> buildGroups(Context context, DataNode input, Meta actionMeta) {
        Meta meta = inputMeta(context, input.meta(), actionMeta);
        List<DataNode<PointSet>> groups;
        if (meta.hasValue("grouping.byValue")) {
            groups = super.buildGroups(context, input, actionMeta);
        } else {
            groups = GroupBuilder.byValue(MERGE_NAME, meta.getString(MERGE_NAME, "merge")).group(input);
        }
        return groups;
    }

    @Override
    protected PointSet execute(Context context, Logable log, String nodeName, Map<String, PointSet> data, Meta meta) {
        PointSet res = mergeDataSets(nodeName, data.values());
        return new ListPointSet(res.getFormat(),res.sort("Uset", true));
    }

    @Override
    protected void afterGroup(Context context, Logable log, String groupName, Meta outputMeta, PointSet output) {
        OutputStream stream = buildActionOutput(context, groupName);
        ColumnedDataWriter.writeDataSet(stream, output, outputMeta.toString());
    }

    @Override
    protected MetaBuilder outputMeta(DataNode<PointSet> input) {

        String numassPath = input.stream().<String>map(item -> item.getValue().meta().getString("numass.path", null))
                .reduce("", (String path, String newPath) -> {
                    if (path == null) {
                        return null;
                    } else if (path.isEmpty()) {
                        return newPath;
                    } else if (!path.equals(newPath)) {
                        return null;
                    } else {
                        return newPath;
                    }
                });

        MetaBuilder builder = super.outputMeta(input);
        /*
         * Указываем путь только есл0и он одинаковый для всех входных файлов
         */
        if (numassPath != null) {
            builder.putValue("numass.path", numassPath);
        }
        return builder;
    }

    private DataPoint mergeDataPoints(DataPoint dp1, DataPoint dp2) {
        if (dp1 == null) {
            return dp2;
        }
        if (dp2 == null) {
            return dp1;
        }

        double Uset = dp1.getValue(parnames[0]).doubleValue();
        //усредняем измеренное напряжение
        double Uread = (dp1.getValue(parnames[1]).doubleValue() + dp2.getValue(parnames[1]).doubleValue()) / 2;

        double t1 = dp1.getValue("Length").doubleValue();
        double t2 = dp2.getValue("Length").doubleValue();
        double time = t1 + t2;

        long total = dp1.getValue(parnames[3]).intValue() + dp2.getValue(parnames[3]).intValue();
        long wind = dp1.getValue(parnames[4]).intValue() + dp2.getValue(parnames[4]).intValue();
        double corr = dp1.getValue(parnames[5]).doubleValue() + dp2.getValue(parnames[5]).doubleValue();

        double cr1 = dp1.getValue("CR").doubleValue();
        double cr2 = dp2.getValue("CR").doubleValue();

        double cr = (cr1 * t1 + cr2 * t2) / (t1 + t2);

        double err1 = dp1.getDouble("CRerr");
        double err2 = dp2.getDouble("CRerr");

        // абсолютные ошибки складываются квадратично
        double crErr = Math.sqrt(err1 * err1 * t1 * t1 + err2 * err2 * t2 * t2) / time;

        MapPoint map = new MapPoint(parnames, Uset, Uread, time, total, wind, corr, cr, crErr);

        if (dp1.names().contains("relCR") && dp2.names().contains("relCR")) {
            double relCR = (dp1.getDouble("relCR") + dp2.getDouble("relCR")) / 2;
            map.putValue("relCR", relCR);
            map.putValue("relCRerr", crErr * relCR / cr);
        }

        return map;
    }

    private PointSet mergeDataSets(String name, Collection<PointSet> ds) {
        //Сливаем все точки в один набор данных
        Map<Double, List<DataPoint>> points = new LinkedHashMap<>();
        for (PointSource d : ds) {
            if (!d.getFormat().contains(parnames)) {
                throw new IllegalArgumentException();
            }
            for (DataPoint dp : d) {
                double uset = dp.getValue(parnames[0]).doubleValue();
                if (!points.containsKey(uset)) {
                    points.put(uset, new ArrayList<>());
                }
                points.get(uset).add(dp);
            }
        }

        List<DataPoint> res = new ArrayList<>();

        points.entrySet().stream().map((entry) -> {
            DataPoint curPoint = null;
            for (DataPoint newPoint : entry.getValue()) {
                curPoint = mergeDataPoints(curPoint, newPoint);
            }
            return curPoint;
        }).forEach((curPoint) -> {
            res.add(curPoint);
        });

        return new ListPointSet(res);

    }

}
