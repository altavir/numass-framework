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
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.XMLMetaWriter;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.*;
import inr.numass.data.NumassData;
import inr.numass.data.NumassPoint;
import inr.numass.data.PointBuilders;
import inr.numass.data.RawNMPoint;
import inr.numass.debunch.DebunchReport;
import inr.numass.debunch.FrameAnalizer;
import inr.numass.storage.NumassDataLoader;
import inr.numass.utils.ExpressionUtils;

import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static inr.numass.utils.TritiumUtils.pointExpression;

/**
 * @author Darksnake
 */
@TypedActionDef(name = "prepareData", inputType = NumassData.class, outputType = Table.class)
@ValueDef(name = "lowerWindow", type = "NUMBER", def = "0", info = "Base for the window lowerWindow bound")
@ValueDef(name = "lowerWindowSlope", type = "NUMBER", def = "0", info = "Slope for the window lowerWindow bound")
@ValueDef(name = "upperWindow", type = "NUMBER", info = "Upper bound for window")
@ValueDef(name = "deadTime", type = "[NUMBER, STRING]", info = "Dead time in s. Could be an expression.")
@ValueDef(name = "correction",
        info = "An expression to correct count number depending on potential `U`, point length `T` and point itself as `point`")
@ValueDef(name = "utransform", info = "Expression for voltage transformation. Uses U as input")
@NodeDef(name = "correction", multiple = true, target = "method::inr.numass.actions.PrepareDataAction.makeCorrection")
public class PrepareDataAction extends OneToOneAction<NumassData, Table> {

    public static String[] parnames = {"Uset", "Uread", "Length", "Total", "Window", "Corr", "CR", "CRerr", "Timestamp"};

    private int getLowerBorder(Meta meta, double Uset) throws ContentException {
        double b = meta.getDouble("lowerWindow", 0);
        double a = meta.getDouble("lowerWindowSlope", 0);

        return Math.max((int) (b + Uset * a), 0);
    }

    @Override
    protected ListTable execute(Context context, String name, NumassData dataFile, Laminate meta) {
//        log.report("File %s started", dataFile.getName());

        int upper = meta.getInt("upperWindow", RawNMPoint.MAX_CHANEL - 1);

        List<Correction> corrections = new ArrayList<>();
        if (meta.hasValue("deadTime")) {
            corrections.add(new DeadTimeCorrection(meta.getString("deadTime")));
        }

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

        if(meta.hasMeta("debunch")){
            if(dataFile instanceof NumassDataLoader){
                dataFile = ((NumassDataLoader) dataFile).applyRawTransformation(raw->debunch(context,raw,meta.getMeta("debunch")));
            } else {
                throw new RuntimeException("Debunch not available");
            }
        }

        List<DataPoint> dataList = new ArrayList<>();
        for (NumassPoint point : dataFile) {

            long total = point.getTotalCount();
            double uset = utransform.apply(point.getVoltage());
            double uread = utransform.apply(point.getVoltage());
            double time = point.getLength();
            int a = getLowerBorder(meta, uset);
            int b = Math.min(upper, RawNMPoint.MAX_CHANEL);

            // count in window
            long wind = point.getCountInWindow(a, b);

            double correctionFactor = corrections.stream()
                    .mapToDouble(cor -> cor.corr(point))
                    .reduce((d1, d2) -> d1 * d2).orElse(1);
            double relativeCorrectionError = Math.sqrt(
                    corrections.stream()
                            .mapToDouble(cor -> cor.relativeErr(point))
                            .reduce((d1, d2) -> d1 * d1 + d2 * d2).orElse(0)
            );

            double cr = wind / point.getLength() * correctionFactor;
            double crErr;
            if (relativeCorrectionError == 0) {
                crErr = Math.sqrt(wind) / point.getLength() * correctionFactor;
            } else {
                crErr = Math.sqrt(1d / wind + Math.pow(relativeCorrectionError, 2)) * cr;
            }

            Instant timestamp = point.getStartTime();

            dataList.add(new MapPoint(parnames, new Object[]{uset, uread, time, total, wind, correctionFactor, cr, crErr, timestamp}));
        }

        TableFormat format;

        if (!dataList.isEmpty()) {
            //Генерируем автоматический формат по первой строчке
            format = TableFormat.forPoint(dataList.get(0));
        } else {
            format = TableFormat.forNames(parnames);
        }

        String head;
        if (dataFile.meta() != null) {
            head = dataFile.meta().toString();
        } else {
            head = dataFile.getName();
        }
        head = head + "\n" + new XMLMetaWriter().writeString(meta) + "\n";

        ListTable data = new ListTable(format, dataList);

        OutputStream stream = buildActionOutput(context, name);

        ColumnedDataWriter.writeTable(stream, data, head);
//        log.logString("File %s completed", dataFile.getName());
        return data;
    }


    @ValueDef(name = "value", type = "[NUMBER, STRING]", info = "Value or function to multiply count rate")
    @ValueDef(name = "err", type = "[NUMBER, STRING]", info = "error of the value")
    private Correction makeCorrection(Meta corrMeta) {
        final String expr = corrMeta.getString("value");
        final String errExpr = corrMeta.getString("err", "");
        return new Correction() {
            @Override
            public double corr(NumassPoint point) {
                return pointExpression(expr, point);
            }

            @Override
            public double corrErr(NumassPoint point) {
                if (errExpr.isEmpty()) {
                    return 0;
                } else {
                    return pointExpression(errExpr, point);
                }
            }
        };
    }

    private NumassPoint debunch(Context context, RawNMPoint point, Meta meta) {
        int upper = meta.getInt("upperchanel", RawNMPoint.MAX_CHANEL);
        int lower = meta.getInt("lowerchanel", 0);
        double rejectionprob = meta.getDouble("rejectprob", 1e-10);
        double framelength = meta.getDouble("framelength", 1);
        double maxCR = meta.getDouble("maxcr", 500d);

        double cr = point.selectChanels(lower, upper).getCR();
        if (cr < maxCR) {
            DebunchReport report = new FrameAnalizer(rejectionprob, framelength, lower, upper).debunchPoint(point);
            return PointBuilders.readRawPoint(report.getPoint());
        } else {
            return PointBuilders.readRawPoint(point);
        }
    }


    private interface Correction {
        /**
         * correction coefficient
         *
         * @param point
         * @return
         */
        double corr(NumassPoint point);

        /**
         * correction coefficient uncertainty
         *
         * @param point
         * @return
         */
        default double corrErr(NumassPoint point) {
            return 0;
        }

        default double relativeErr(NumassPoint point) {
            double corrErr = corrErr(point);
            if (corrErr == 0) {
                return 0;
            } else {
                return corrErr / corr(point);
            }
        }
    }

    private class DeadTimeCorrection implements Correction {

        private final Function<NumassPoint, Double> deadTimeFunction;

        public DeadTimeCorrection(String expr) {
            deadTimeFunction = point -> pointExpression(expr, point);
        }

        @Override
        public double corr(NumassPoint point) {
            double deadTime = deadTimeFunction.apply(point);
            if (deadTime > 0) {
                double factor = deadTime / point.getLength() * point.getTotalCount();
//            double total = point.getTotalCount();
//            double time = point.getLength();
//            return 1d/(1d - factor);

                return (1d - Math.sqrt(1d - 4d * factor)) / 2d / factor;
            } else {
                return 1d;
            }
        }
    }


}
