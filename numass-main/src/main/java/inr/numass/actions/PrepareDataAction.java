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
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.XMLMetaWriter;
import hep.dataforge.io.reports.Reportable;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableFormat;
import inr.numass.storage.NMPoint;
import inr.numass.storage.NumassData;
import inr.numass.storage.RawNMPoint;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "prepareData", inputType = NumassData.class, outputType = Table.class)
@ValueDef(name = "lowerWindow", type = "NUMBER", def = "0", info = "Base for the window lowerWindow bound")
@ValueDef(name = "lowerWindowSlope", type = "NUMBER", def = "0", info = "Slope for the window lowerWindow bound")
@ValueDef(name = "upperWindow", type = "NUMBER", info = "Upper bound for window")
@ValueDef(name = "deadTime", type = "NUMBER", def = "0", info = "Dead time in us")
public class PrepareDataAction extends OneToOneAction<NumassData, Table> {

    public static String[] parnames = {"Uset", "Uread", "Length", "Total", "Window", "Corrected", "CR", "CRerr", "Timestamp"};

    private int getLowerBorder(Meta meta, double Uset) throws ContentException {
        double b = meta.getDouble("lowerWindow", 0);
        double a = meta.getDouble("lowerWindowSlope", 0);

        return (int) (b + Uset * a);
    }

    @Override
    protected ListTable execute(Context context, Reportable log, String name, Laminate meta, NumassData dataFile) {
//        log.report("File %s started", dataFile.getName());

        int upper = meta.getInt("upperWindow", RawNMPoint.MAX_CHANEL - 1);

        double deadTime = meta.getDouble("deadTime", 0);
//        double bkg = source.meta().getDouble("background", this.meta().getDouble("background", 0));

        List<DataPoint> dataList = new ArrayList<>();
        for (NMPoint point : dataFile.getNMPoints()) {

            long total = point.getEventsCount();
            double Uset = point.getUset();
            double Uread = point.getUread();
            double time = point.getLength();
            int a = getLowerBorder(meta, Uset);
            int b = Math.min(upper, RawNMPoint.MAX_CHANEL);

//            analyzer.setMonitorCorrector(corrector);
            long wind = point.getCountInWindow(a, b);

            double corr = point.getCountRate(a, b, deadTime) * point.getLength();// - bkg * (b - a);

            double cr = point.getCountRate(a, b, deadTime);
            double crErr = point.getCountRateErr(a, b, deadTime);

            Instant timestamp = point.getStartTime();

            dataList.add(new MapPoint(parnames, new Object[]{Uset, Uread, time, total, wind, corr, cr, crErr, timestamp}));
        }

        TableFormat format;

        if (!dataList.isEmpty()) {
            //Генерируем автоматический формат по первой строчке
            format = TableFormat.forPoint(dataList.get(0));
        } else {
            format = TableFormat.fixedWidth(8, parnames);
        }

        String head;
        if (dataFile.meta() != null) {
            head = dataFile.meta().toString();
        } else {
            head = dataFile.getName();
        }
        head = head + "\n" + new XMLMetaWriter().writeString(meta, null) + "\n";

        ListTable data = new ListTable(format, dataList);

        OutputStream stream = buildActionOutput(context, name);

        ColumnedDataWriter.writeDataSet(stream, data, head);
//        log.logString("File %s completed", dataFile.getName());
        return data;
    }

}
