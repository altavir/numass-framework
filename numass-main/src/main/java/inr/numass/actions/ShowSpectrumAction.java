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
import hep.dataforge.plots.fx.FXPlotUtils;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.log.Logable;
import hep.dataforge.meta.Meta;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import inr.numass.data.ESpectrum;
import inr.numass.data.NMFile;
import inr.numass.data.NMPoint;
import java.awt.Color;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "showSpectrum", inputType = NMFile.class, outputType = NMFile.class)
public class ShowSpectrumAction extends OneToOneAction<NMFile, NMFile> {

    public ShowSpectrumAction(Context context, Meta an) {
        super(context, an);
    }

    @Override
    protected NMFile execute(Logable log, Meta reader, NMFile source) throws ContentException {
        log.log("File {} started", source.getName());

        List<NMPoint> printPoints = new ArrayList<>();
        List<NMPoint> showPoints = new ArrayList<>();

        for (NMPoint point : source.getNMPoints()) {
            if (toPrint(point)) {
                printPoints.add(point);
            }
            if (toShow(point)) {
                showPoints.add(point);
            }
        }

        int chanelsPerBin = meta().getInt("binning", 1); // биннинг
        boolean normalize = meta().getBoolean("normalize", false); // нормировка на время набора точки

        if (showPoints.size() > 0) {
            showSpectra(showPoints, source.getName(), chanelsPerBin, normalize);
        }

        if (printPoints.size() > 0) {
            ESpectrum data = new ESpectrum(source.getName(), printPoints, chanelsPerBin, normalize);

            OutputStream stream = buildActionOutput(data);

            ColumnedDataWriter.writeDataSet(stream, data, source.getName());

        }

        log.log("File {} completed", source.getName());
        return source;
    }

    private boolean toPrint(NMPoint point) throws ContentException {
        Meta root = this.meta();

        if (root.hasNode("print")) {
            List<? extends Meta> cfg = meta().getNodes("print");
            boolean res = false;
            for (Meta e : cfg) {
                double from = e.getDouble("from", 0);
                double to = e.getDouble("to", Double.POSITIVE_INFINITY);
                res = res || ((point.getUset() >= from) && (point.getUset() <= to));
            }
            return res;
        } else {
            return root.getBoolean("print", false);
        }
    }

    private boolean toShow(NMPoint point) throws ContentException {
        Meta root = this.meta();

        if (root.hasNode("show")) {
            List<? extends Meta> cfg = meta().getNodes("show");
            boolean res = false;
            for (Meta e : cfg) {
                double from = e.getDouble("from", 0);
                double to = e.getDouble("to", Double.POSITIVE_INFINITY);
                res = res || ((point.getUset() >= from) && (point.getUset() <= to));
            }
            return res;
        } else {
            return root.getBoolean("show", false);
        }
    }

    private static void showSpectra(List<NMPoint> points, String head, int binning, boolean normalize) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        for (NMPoint point : points) {
            Map<Double, Double> spectrum = point.getMapWithBinning(binning, normalize);

            String serName = Double.toString(point.getUset());
            XYSeries ser;
            if (dataset.getSeriesIndex(serName) >= 0) {
                serName = serName + " " + Integer.toString(points.indexOf(point));
            }
            ser = new XYSeries(serName);
            for (Map.Entry<Double, Double> sp : spectrum.entrySet()) {
                ser.add(sp.getKey(), sp.getValue());
            }

            dataset.addSeries(ser);
        }

        showSpectra(dataset, head, binning, normalize);

    }

    private static void showSpectra(XYSeriesCollection dataset, String head, int binning, boolean normalize) {

        String axisName = "count";
        if (normalize) {
            axisName += " rate";
        } else {
            axisName += " number";
        }

        if (binning != 1) {
            axisName += " per " + binning + " chanels";
        }

        JFreeChartFrame frame = FXPlotUtils.displayJFreeChart(head, null);
        
        frame.getYAxisConfig().putValue("title", axisName);

        JFreeChart chart = frame.getChart();
        
        chart.getXYPlot().setDataset(dataset);
        
        chart.getXYPlot().setRenderer(new XYLineAndShapeRenderer(true, false));

        chart.getLegend().setPosition(RectangleEdge.RIGHT);
        chart.getXYPlot().setDomainGridlinesVisible(true);
        chart.getXYPlot().setRangeGridlinesVisible(true);
        chart.getXYPlot().setDomainGridlinePaint(Color.BLACK);
        chart.getXYPlot().setRangeGridlinePaint(Color.BLACK);
    }

}
