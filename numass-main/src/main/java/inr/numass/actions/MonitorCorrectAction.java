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

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.values.ValueFactory;
import hep.dataforge.values.ValueMap;
import hep.dataforge.values.Values;
import inr.numass.NumassUtils;
import inr.numass.data.analyzers.NumassAnalyzer;
import inr.numass.data.api.NumassPoint;
import javafx.util.Pair;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Darksnake
 */
@TypedActionDef(name = "monitor", inputType = Table.class, outputType = Table.class)
public class MonitorCorrectAction extends OneToOneAction<Table, Table> {

    //private static final String[] monitorNames = {"timestamp", NumassAnalyzer.COUNT_KEY, NumassAnalyzer.COUNT_RATE_KEY, NumassAnalyzer.COUNT_RATE_KEY};

    private CopyOnWriteArrayList<Values> monitorPoints = new CopyOnWriteArrayList<>();
    //FIXME remove from state

    @Override
    protected Table execute(Context context, String name, Table sourceData, Laminate meta) throws ContentException {

        double monitor = meta.getDouble("monitorPoint", Double.NaN);

        TreeMap<Instant, Values> index = getMonitorIndex(monitor, sourceData);
        if (index.isEmpty()) {
            context.getHistory().getChronicle(name).reportError("No monitor points found");
            return sourceData;
        }
        double norm = 0;
//        double totalAv = 0;
//        StringBuilder head = new StringBuilder();
//        head.append(String.format("%20s\t%10s\t%s%n", "timestamp", "Count", "CR in window"));
//        for (Values dp : index.values()) {
//            head.append(String.format("%20s\t%10d\t%g%n", getTime(dp).toString(), getTotal(dp), getCR(dp)));
//            norm += getCR(dp) / index.size();
//            totalAv += getTotal(dp) / index.size();
//            monitorPoints.add(dp);
//        }
//
//        head.append(String.format("%20s\t%10g\t%g%n", "Average", totalAv, norm));

        List<Values> dataList = new ArrayList<>();

        for (Values dp : sourceData) {
            ValueMap.Builder pb = new ValueMap.Builder(dp);
            pb.putValue("Monitor", 1.0);
            if (!isMonitorPoint(monitor, dp) || index.isEmpty()) {
                Pair<Double, Double> corr;
                if (meta.getBoolean("spline", false)) {
                    corr = getSplineCorrection(index, dp, norm);
                } else {
                    corr = getLinearCorrection(index, dp, norm);
                }
                double corrFactor = corr.getKey();
                double corrErr = corr.getValue();

                double pointErr = dp.getValue(NumassAnalyzer.COUNT_RATE_ERROR_KEY).getDouble() / getCR(dp);
                double err = Math.sqrt(corrErr * corrErr + pointErr * pointErr) * getCR(dp);

                if (dp.getNames().contains("Monitor")) {
                    pb.putValue("Monitor", ValueFactory.of(dp.getValue("Monitor").getDouble() / corrFactor));
                } else {
                    pb.putValue("Monitor", corrFactor);
                }

                pb.putValue(NumassAnalyzer.COUNT_RATE_KEY, ValueFactory.of(dp.getValue(NumassAnalyzer.COUNT_RATE_KEY).getDouble() / corrFactor));
                pb.putValue(NumassAnalyzer.COUNT_RATE_ERROR_KEY, ValueFactory.of(err));
            } else {
                double corrFactor = dp.getValue(NumassAnalyzer.COUNT_RATE_KEY).getDouble() / norm;
                if (dp.getNames().contains("Monitor")) {
                    pb.putValue("Monitor", ValueFactory.of(dp.getValue("Monitor").getDouble() / corrFactor));
                } else {
                    pb.putValue("Monitor", corrFactor);
                }
                pb.putValue(NumassAnalyzer.COUNT_RATE_KEY, norm);

            }

            if (meta.getBoolean("calculateRelative", false)) {
                pb.putValue("relCR", pb.build().getValue(NumassAnalyzer.COUNT_RATE_KEY).getDouble() / norm);
                pb.putValue("relCRerr", pb.build().getValue(NumassAnalyzer.COUNT_RATE_ERROR_KEY).getDouble() / norm);
            }

            dataList.add(pb.build());
        }

//        DataFormat format;
//
//        if (!dataList.isEmpty()) {
//            //Генерируем автоматический формат по первой строчке
//            format = DataFormat.of(dataList.getPoint(0));
//        } else {
//            format = DataFormat.of(parnames);
//        }
        Table res = ListTable.infer(dataList);

        context.getIo().output(name, getName()).render(NumassUtils.INSTANCE.wrap(res, meta), Meta.empty());

        return res;
    }

    private Pair<Double, Double> getSplineCorrection(TreeMap<Instant, Values> index, Values dp, double norm) {
        double time = getTime(dp).toEpochMilli();

        double[] xs = new double[index.size()];
        double[] ys = new double[index.size()];

        int i = 0;

        for (Entry<Instant, Values> entry : index.entrySet()) {
            xs[i] = (double) entry.getKey().toEpochMilli();
            ys[i] = getCR(entry.getValue()) / norm;
            i++;
        }

        PolynomialSplineFunction corrFunc = new SplineInterpolator().interpolate(xs, ys);
        if (corrFunc.isValidPoint(time)) {
            double averageErr = index.values().stream().mapToDouble(p -> p.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY)).average().getAsDouble();
            return new Pair<>(corrFunc.value(time), averageErr / norm / 2d);
        } else {
            return new Pair<>(1d, 0d);
        }
    }

    private Pair<Double, Double> getLinearCorrection(TreeMap<Instant, Values> index, Values dp, double norm) {
        Instant time = getTime(dp);
        Entry<Instant, Values> previousMonitor = index.floorEntry(time);
        Entry<Instant, Values> nextMonitor = index.ceilingEntry(time);

        if (previousMonitor == null) {
            previousMonitor = nextMonitor;
        }

        if (nextMonitor == null) {
            nextMonitor = previousMonitor;
        }

        double p;
        if (nextMonitor.getKey().isAfter(time) && time.isAfter(previousMonitor.getKey())) {
            p = 1.0 * (time.toEpochMilli() - previousMonitor.getKey().toEpochMilli())
                    / (nextMonitor.getKey().toEpochMilli() - previousMonitor.getKey().toEpochMilli());
        } else {
            p = 0.5;
        }

        double corrFactor = (getCR(previousMonitor.getValue()) * (1 - p) + getCR(nextMonitor.getValue()) * p) / norm;
        double corrErr = previousMonitor.getValue().getValue(NumassAnalyzer.COUNT_RATE_ERROR_KEY).getDouble() / getCR(previousMonitor.getValue()) / Math.sqrt(2);
        return new Pair<>(corrFactor, corrErr);
    }

    @Override
    protected void afterAction(Context context, String name, Table res, Laminate meta) {
        printMonitorData(context, meta);
        super.afterAction(context, name, res, meta);
    }

    private void printMonitorData(Context context, Meta meta) {
        if (!monitorPoints.isEmpty()) {
            String monitorFileName = meta.getString("monitorFile", "monitor");
            ListTable data = ListTable.infer(monitorPoints);

            context.getIo().output(monitorFileName, getName()).render(NumassUtils.INSTANCE.wrap(data, meta), Meta.empty());
//            ColumnedDataWriter.writeTable(stream, TableTransform.sort(data, "Timestamp", true), "Monitor points", monitorNames);
        }
    }

    private boolean isMonitorPoint(double monitor, Values point) {
        return point.getValue(NumassPoint.HV_KEY).getDouble() == monitor;
    }

    private Instant getTime(Values point) {
        return point.getValue(NumassAnalyzer.TIME_KEY).getTime();
    }

    private int getTotal(Values point) {
        return point.getValue(NumassAnalyzer.COUNT_KEY).getInt();
    }

    private double getCR(Values point) {
        return point.getValue(NumassAnalyzer.COUNT_RATE_KEY).getDouble();
    }

    private TreeMap<Instant, Values> getMonitorIndex(double monitor, Iterable<Values> data) {
        TreeMap<Instant, Values> res = new TreeMap<>();
        for (Values dp : data) {
            if (isMonitorPoint(monitor, dp)) {
                res.put(getTime(dp), dp);
            }
        }
        return res;
    }

}
