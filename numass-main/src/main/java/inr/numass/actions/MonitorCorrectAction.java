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
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.Value;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import hep.dataforge.tables.PointSource;
import hep.dataforge.tables.Table;
import hep.dataforge.io.reports.Reportable;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "monitor", inputType = Table.class, outputType = Table.class)
@ValueDef(name = "monitorPoint", type = "NUMBER", required = true, info = "The Uset for monitor point")
@ValueDef(name = "monitorFile", info = "The outputfile for monitor points", def = "monitor.out")
@ValueDef(name = "calculateRelative", info = "Calculate count rate relative to average monitor point", def = "false")
public class MonitorCorrectAction extends OneToOneAction<Table, Table> {

    private static final String[] monitorNames = {"Timestamp", "Total", "CR", "CRerr"};

    CopyOnWriteArrayList<DataPoint> monitorPoints = new CopyOnWriteArrayList<>();
    //FIXME remove from state

    @Override
    protected Table execute(Context context, Reportable log, String name, Laminate meta, Table sourceData) throws ContentException {

        double monitor = meta.getDouble("monitorPoint", Double.NaN);

        TreeMap<LocalDateTime, DataPoint> index = getMonitorIndex(monitor, sourceData);
        if (index.isEmpty()) {
            log.reportError("No monitor points found");
            return sourceData;
        }
        double norm = 0;
        double totalAv = 0;
        String head = "";
        head += String.format("%20s\t%10s\t%s%n", "Timestamp", "Total", "CR in window");
        for (DataPoint dp : index.values()) {
            head += String.format("%20s\t%10d\t%g%n", getTime(dp).toString(), getTotal(dp), getCR(dp));
            norm += getCR(dp) / index.size();
            totalAv += getTotal(dp) / index.size();
            monitorPoints.add(dp);
        }

        head += String.format("%20s\t%10g\t%g%n", "Average", totalAv, norm);

        List<DataPoint> dataList = new ArrayList<>();

        for (DataPoint dp : sourceData) {
            MapPoint.Builder pb = new MapPoint.Builder(dp);
            pb.putValue("Monitor", 1.0);
            if (!isMonitorPoint(monitor, dp) || index.isEmpty()) {
                LocalDateTime time = getTime(dp);
                Entry<LocalDateTime, DataPoint> previousMonitor = index.floorEntry(time);
                Entry<LocalDateTime, DataPoint> nextMonitor = index.ceilingEntry(time);

                if (previousMonitor == null) {
                    previousMonitor = nextMonitor;
                }

                if (nextMonitor == null) {
                    nextMonitor = previousMonitor;
                }

                double p;
//                p = (getTime(dp).toEpochMilli() - previousMonitor.getKey().toEpochMilli())/
                p = 0.5;

                double corrFactor = (getCR(previousMonitor.getValue()) * p + getCR(nextMonitor.getValue()) * (1 - p)) / norm;
                double corrErr = previousMonitor.getValue().getValue("CRerr").doubleValue() / getCR(previousMonitor.getValue());
                double pointErr = dp.getValue("CRerr").doubleValue() / getCR(dp);
                double err = Math.sqrt(corrErr * corrErr + pointErr * pointErr) * getCR(dp);

                if (dp.names().contains("Monitor")) {
                    pb.putValue("Monitor", Value.of(dp.getValue("Monitor").doubleValue() / corrFactor));
                } else {
                    pb.putValue("Monitor", corrFactor);
                }
                pb.putValue("CR", Value.of(dp.getValue("CR").doubleValue() / corrFactor));
                pb.putValue("Window", Value.of(dp.getValue("Window").doubleValue() / corrFactor));
                pb.putValue("Corrected", Value.of(dp.getValue("Corrected").doubleValue() / corrFactor));
                pb.putValue("CRerr", Value.of(err));
            }
            if (meta.getBoolean("calculateRelative", false)) {
                pb.putValue("relCR", pb.build().getValue("CR").doubleValue() / norm);
                pb.putValue("relCRerr", pb.build().getValue("CRerr").doubleValue() / norm);
            }
            dataList.add(pb.build());
        }

//        DataFormat format;
//
//        if (!dataList.isEmpty()) {
//            //Генерируем автоматический формат по первой строчке
//            format = DataFormat.of(dataList.getRow(0));
//        } else {
//            format = DataFormat.of(parnames);
//        }
        Table data = new ListTable(dataList);

        OutputStream stream = buildActionOutput(context, name);

        ColumnedDataWriter.writeDataSet(stream, data, head);

        return data;
    }

    @Override
    protected void afterAction(Context context, String name, Table res, Laminate meta) {
        printMonitorData(context, meta);
        super.afterAction(context, name, res, meta);
    }

    private void printMonitorData(Context context, Meta meta) {
        String monitorFileName = meta.getString("monitorFile", "monitor");
        OutputStream stream = buildActionOutput(context, monitorFileName);
        ListTable data = new ListTable(monitorPoints);
        ColumnedDataWriter.writeDataSet(stream, data.sort("Timestamp", true), "Monitor points", monitorNames);
    }

    private boolean isMonitorPoint(double monitor, DataPoint point) {
        return point.getValue("Uset").doubleValue() == monitor;
    }

    private LocalDateTime getTime(DataPoint point) {
        return LocalDateTime.ofInstant(point.getValue("Timestamp").timeValue(), ZoneId.of("GMT+3"));
    }

    private int getTotal(DataPoint point) {
        return point.getValue("Total").intValue();
    }

    private double getCR(DataPoint point) {
        return point.getValue("CR").doubleValue();
    }

    private TreeMap<LocalDateTime, DataPoint> getMonitorIndex(double monitor, Iterable<DataPoint> data) {
        TreeMap<LocalDateTime, DataPoint> res = new TreeMap<>();
        for (DataPoint dp : data) {
            if (isMonitorPoint(monitor, dp)) {
                res.put(getTime(dp), dp);
            }
        }
        return res;
    }

}
