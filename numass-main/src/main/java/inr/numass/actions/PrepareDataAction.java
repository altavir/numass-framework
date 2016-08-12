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
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.XMLMetaWriter;
import hep.dataforge.io.reports.Reportable;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.*;
import inr.numass.storage.NMPoint;
import inr.numass.storage.NumassData;
import inr.numass.storage.RawNMPoint;
import inr.numass.utils.TritiumUtils;
import inr.numass.utils.UnderflowCorrection;

import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static inr.numass.utils.TritiumUtils.evaluateExpression;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "prepareData", inputType = NumassData.class, outputType = Table.class)
@ValueDef(name = "lowerWindow", type = "NUMBER", def = "0", info = "Base for the window lowerWindow bound")
@ValueDef(name = "lowerWindowSlope", type = "NUMBER", def = "0", info = "Slope for the window lowerWindow bound")
@ValueDef(name = "upperWindow", type = "NUMBER", info = "Upper bound for window")
@ValueDef(name = "deadTime", type = "[NUMBER, STRING]", def = "0", info = "Dead time in s. Could be an expression.")
//@ValueDef(name = "underflow", type = "BOOLEAN", def = "true",
//        info = "Enables calculation of detector threshold underflow using exponential shape of energy spectrum tail. "
//        + "Not recomended to use with floating window.")
//@ValueDef(name = "underflow.upperBorder", type = "NUMBER", def = "800", info = "Upper chanel for underflow calculation.")
//@ValueDef(name = "underflow.threshold", type = "NUMBER", def = "17000", info = "The maximum U for undeflow calculation")
//@ValueDef(name = "underflow.function", info = "An expression for underflow correction above threshold")
@ValueDef(name = "correction",
        info = "An expression to correct count tumber depending on potential ${U}, point length ${T} and point itself as ${point}")
public class PrepareDataAction extends OneToOneAction<NumassData, Table> {

    public static String[] parnames = {"Uset", "Uread", "Length", "Total", "Window", "Corr", "CR", "CRerr", "Timestamp"};

    private int getLowerBorder(Meta meta, double Uset) throws ContentException {
        double b = meta.getDouble("lowerWindow", 0);
        double a = meta.getDouble("lowerWindowSlope", 0);

        return Math.max((int) (b + Uset * a), 0);
    }

    @Override
    protected ListTable execute(Reportable log, String name, Laminate meta, NumassData dataFile) {
//        log.report("File %s started", dataFile.getName());

        int upper = meta.getInt("upperWindow", RawNMPoint.MAX_CHANEL - 1);

        Function<NMPoint, Double> deadTimeFunction;
        if (meta.hasValue("deadTime")) {
            deadTimeFunction = point -> evaluateExpression(point, meta.getString("deadTime"));
        } else {
            deadTimeFunction = point -> 0.0;
        }

//        double bkg = source.meta().getDouble("background", this.meta().getDouble("background", 0));
        List<DataPoint> dataList = new ArrayList<>();
        for (NMPoint point : dataFile.getNMPoints()) {

            long total = point.getEventsCount();
            double Uset = point.getUset();
            double Uread = point.getUread();
            double time = point.getLength();
            int a = getLowerBorder(meta, Uset);
            int b = Math.min(upper, RawNMPoint.MAX_CHANEL);

            // count in window
            long wind = point.getCountInWindow(a, b);

            // count rate after all corrections
            double cr = TritiumUtils.countRateWithDeadTime(point, a, b, deadTimeFunction.apply(point));
            // count rate error after all corrections
            double crErr = TritiumUtils.countRateWithDeadTimeErr(point, a, b, deadTimeFunction.apply(point));

            double correctionFactor = correction(log, point, meta);

            cr = cr * correctionFactor;
            crErr = crErr * correctionFactor;

            Instant timestamp = point.getStartTime();

            dataList.add(new MapPoint(parnames, new Object[]{Uset, Uread, time, total, wind, correctionFactor, cr, crErr, timestamp}));
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

        OutputStream stream = buildActionOutput(name);

        ColumnedDataWriter.writeDataSet(stream, data, head);
//        log.logString("File %s completed", dataFile.getName());
        return data;
    }

    /**
     * The factor to correct for count below detector threshold
     *
     * @param log
     * @param point
     * @param meta
     * @return
     */
    private double correction(Reportable log, NMPoint point, Laminate meta) {
        if (meta.hasValue("correction")) {
//            log.report("Using correction from formula: {}", meta.getString("correction"));
            return evaluateExpression(point, meta.getString("correction"));
        } else if (meta.hasNode("underflow")) {
            return new UnderflowCorrection().get(log, meta.getNode("underflow"), point);
        } else {
            return 1;
        }
    }    
    




}
