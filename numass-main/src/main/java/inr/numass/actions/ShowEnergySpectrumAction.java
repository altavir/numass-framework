/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.PlotsPlugin;
import hep.dataforge.plots.XYPlotFrame;
import hep.dataforge.plots.data.PlottableData;
import hep.dataforge.plots.data.XYPlottable;
import hep.dataforge.tables.*;
import hep.dataforge.values.ValueType;
import inr.numass.storage.NMPoint;
import inr.numass.storage.NumassData;

import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Alexander Nozik
 */
@TypedActionDef(inputType = NumassData.class, outputType = Table.class, name = "energySpectrum", info = "Generate output table and optionally plot for detector energy spectra")
public class ShowEnergySpectrumAction extends OneToOneAction<NumassData, Table> {

    @Override
    protected Table execute(Context context, String name, NumassData input, Laminate inputMeta) {
        int binning = inputMeta.getInt("binning", 20);
        boolean normalize = inputMeta.getBoolean("normalize", true);
        List<NMPoint> points = input.getNMPoints();

        if (points.isEmpty()) {
            getLogger(inputMeta).error("Empty data");
            return null;
        }

        //build header
        List<String> names = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            names.add(String.format("%d: %.2f", i, points.get(i).getUset()));
        }

        LinkedHashMap<String, Map<Double, Double>> valueMap = points.stream()
                .collect(Collectors.toMap(
                        p -> names.get(points.indexOf(p)),
                        p -> p.getMapWithBinning(binning, normalize),
                        (v1, v2) -> v1,
                        () -> new LinkedHashMap<>()
                ));

        Collection<Double> rows = valueMap.values().stream().findAny().get().keySet();

        //Building table format
        TableFormatBuilder formatBuilder = new TableFormatBuilder();
        formatBuilder.addColumn("channel",ValueType.NUMBER);
        names.stream().forEach((columnName) -> {
            formatBuilder.addColumn(columnName, ValueType.NUMBER);
        });

        ListTable.Builder builder = new ListTable.Builder(formatBuilder.build());
        rows.stream().forEachOrdered((Double channel) -> {
            MapPoint.Builder mb = new MapPoint.Builder();
            mb.putValue("channel", channel);
            valueMap.entrySet().forEach((Map.Entry<String, Map<Double, Double>> entry) -> {
                mb.putValue(entry.getKey(), entry.getValue().get(channel));
            });
            builder.row(mb.build());
        });

        OutputStream out = buildActionOutput(context, name);
        Table table = builder.build();

        ColumnedDataWriter.writeDataSet(out, table, inputMeta.toString());

        if (inputMeta.hasMeta("plot") || inputMeta.getBoolean("plot", false)) {
            XYPlotFrame frame = (XYPlotFrame) PlotsPlugin
                    .buildFrom(context).buildPlotFrame(getName(), name,
                    inputMeta.getMeta("plot", Meta.empty()));
            fillDetectorData(valueMap).forEach(frame::add);

        }
        return table;
    }

    private List<XYPlottable> fillDetectorData(LinkedHashMap<String, Map<Double, Double>> map) {
        List<XYPlottable> plottables = new ArrayList<>();
        Meta plottableConfig = new MetaBuilder("plot")
                .setValue("connectionType", "step")
                .setValue("thickness", 2)
                .setValue("showLine", true)
                .setValue("showSymbol", false)
                .setValue("showErrors", false)
                .build();

        int index = 0;
        for (Map.Entry<String, Map<Double, Double>> entry : map.entrySet()) {
            index++;
            String seriesName = String.format("%d: %s", index, entry.getKey());

            String[] nameList = {XYAdapter.X_VALUE_KEY, XYAdapter.Y_VALUE_KEY};
            List<DataPoint> data = entry.getValue().entrySet().stream()
                    .map(e -> new MapPoint(nameList, e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            PlottableData datum = PlottableData.plot(seriesName, XYAdapter.DEFAULT_ADAPTER, data);
            datum.configure(plottableConfig);
            plottables.add(datum);
        }
        return plottables;

    }

}
