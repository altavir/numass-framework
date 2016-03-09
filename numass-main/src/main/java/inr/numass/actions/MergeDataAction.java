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

import hep.dataforge.actions.ManyToOneAction;
import hep.dataforge.actions.GroupBuilder;
import hep.dataforge.content.NamedGroup;
import hep.dataforge.context.Context;
import hep.dataforge.points.DataPoint;
import hep.dataforge.points.ListPointSet;
import hep.dataforge.points.MapPoint;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.log.Logable;
import hep.dataforge.meta.Meta;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import hep.dataforge.points.PointSet;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "merge", inputType = PointSet.class, outputType = PointSet.class, description = "Merge different numass data files into one.")
@NodeDef(name = "grouping", info = "The defenition of grouping rule for this merge", target = "method::hep.dataforge.content.GroupBuilder.byAnnotation")
//@Parameter(name = "groupBy", def = "mergeTag", info = "Defines the name of the value by which grouping is made. The value is supposed to be a String, but in practice could be any type which could be converted to String.")
public class MergeDataAction extends ManyToOneAction<PointSet, PointSet> {

    public static final String MERGE_NAME = "mergeName";
    public static String[] parnames = {"Uset", "Uread", "Length", "Total", "Window", "Corrected", "CR", "CRerr"};

    public MergeDataAction(Context context, Meta an) {
        super(context, an);
    }

    @Override
    protected List<NamedGroup<PointSet>> buildGroups(Meta reader, List<PointSet> input) {
        List<NamedGroup<PointSet>> groups;
        if (reader.hasValue("grouping.byValue")) {
            groups = super.buildGroups(reader, input);
        } else {
            groups = GroupBuilder.byValue(MERGE_NAME, reader.getString(MERGE_NAME, "merge")).group(input);
        }
        return groups;
    }

    @Override
    protected PointSet execute(Logable log, Meta reader, NamedGroup<PointSet> input) {
        return mergeOne(log, input.getName(), input.asList());
//        List<DataSet> res = new ArrayList<>();
//        for (NamedGroup<DataSet> buildGroups : groups) {
//            res.add(mergeOne(log, buildGroups.getName(), buildGroups.asList()));
//        }
//        return new ContentList<>(input.getName(), PointSet.class, res);
    }

    private PointSet mergeOne(Logable log, String fileName, List<PointSet> files) {
        PointSet[] data = new PointSet[files.size()];
        String head = "Numass data merge\n";

        String numassPath = "";

        /*
         * Проверяем являются ли пути одинаковыми у всех файлов 
         * TODO не изящное решение
         */
        for (int i = 0; i < files.size(); i++) {
            data[i] = files.get(i);
            head += "\t" + data[i].getName() + "\n";
            if (numassPath != null) {
                String newPath = data[i].meta().getString("numass.path", null);
                if (numassPath.isEmpty()) {
                    numassPath = newPath;
                } else {
                    if (!numassPath.equals(newPath)) {
                        numassPath = null;
                    }
                }
            }
        }

        PointSet res = mergeDataSets(fileName, data);

        /*
         * Указываем путь только если он одинаковый для всех входных файлов
         */
        if (numassPath != null) {
            res.setMeta(res.meta().getBuilder().putValue("numass.path", numassPath).build());
        }

        res = res.sort("Uset", true);

        OutputStream stream = buildActionOutput(res);

        ColumnedDataWriter.writeDataSet(stream, res, head);

        return res;
    }

//    private Map<String, List<DataSet>> buildMergeGroups(String mergeBy, NamedGroup<DataSet> input) {
//        Map<String, List<DataSet>> map = new HashMap<>();
//        for (PointSet ds : input) {
//            String tag = ds.meta().getString(mergeBy, meta().getString(mergeBy, "merge"));
//            if (!map.containsKey(tag)) {
//                map.put(tag, new ArrayList<>());
//            }
//            map.get(tag).add(ds);
//        }
//        return map;
//    }
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

    private PointSet mergeDataSets(String name, PointSet... ds) {
        //Сливаем все точки в один набор данных
        Map<Double, List<DataPoint>> points = new LinkedHashMap<>();
        for (PointSet d : ds) {
            if (!d.getDataFormat().contains(parnames)) {
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

        for (Map.Entry<Double, List<DataPoint>> entry : points.entrySet()) {
            DataPoint curPoint = null;
            for (DataPoint newPoint : entry.getValue()) {
                curPoint = mergeDataPoints(curPoint, newPoint);
            }
            res.add(curPoint);
        }

        return new ListPointSet(name, null, res);

    }

}
