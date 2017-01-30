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
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.Laminate;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Value;
import inr.numass.storage.NMFile;
import inr.numass.storage.NMPoint;
import inr.numass.storage.NumassData;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Darksnake
 */
@TypedActionDef(name = "findBorder", inputType = NMFile.class, outputType = Table.class)
public class FindBorderAction extends OneToOneAction<NumassData, Table> {

    private final static String[] names = {"U", "80%", "90%", "95%", "99%"};
    private final static double[] percents = {0.8, 0.9, 0.95, 0.99};

    private UnivariateFunction normCorrection = e -> 1 + 13.265 * Math.exp(-e / 2343.4);

    @Override
    protected Table execute(Context context, String name, NumassData source, Laminate meta) throws ContentException {
        report(context, name, "File {} started", source.getName());

        int upperBorder = meta.getInt("upper", 4094);
        int lowerBorder = meta.getInt("lower", 0);
        double substractReference = meta.getDouble("reference", 0);

        NMPoint referencePoint = null;
        if (substractReference > 0) {
            referencePoint = source.getByUset(substractReference);
            if (referencePoint == null) {
                report(context, name, "Reference point {} not found", substractReference);
            }
        }

        ListTable.Builder dataBuilder = new ListTable.Builder(names);

        fill(dataBuilder, source, lowerBorder, upperBorder, referencePoint);
        Table bData = dataBuilder.build();

        OutputStream stream = buildActionOutput(context, name);

        ColumnedDataWriter.writeDataSet(stream, bData, String.format("%s : lower = %d upper = %d", name, lowerBorder, upperBorder));

        report(context, name, "File {} completed", source.getName());
        return bData;
    }

    private double getNorm(Map<Double, Double> spectrum, int lower, int upper) {
        double res = 0;
        for (Map.Entry<Double, Double> entry : spectrum.entrySet()) {
            if ((entry.getKey() >= lower) && (entry.getKey() <= upper)) {
                res += entry.getValue();
            }
        }

        return res;
    }

    private void fill(ListTable.Builder dataBuilder, NumassData file, int lower, int upper, NMPoint reference) {
        for (NMPoint point : file) {
            if ((reference != null) && (point.getUset() == reference.getUset())) {
                continue;
            }
            //создаем основу для будущей точки
            HashMap<String, Value> map = new HashMap<>();
            map.put(names[0], Value.of(point.getUset()));
            Map<Double, Double> spectrum;
            if (reference != null) {
                spectrum = point.getMapWithBinning(reference, 0);
            } else {
                spectrum = point.getMapWithBinning(0, true);
            }
            double norm = getNorm(spectrum, lower, upper) * normCorrection.value(point.getUset());
            double counter = 0;
            int chanel = upper;
            while (chanel > lower) {
                chanel--;
                counter += spectrum.get((double) chanel);
                for (int i = 0; i < percents.length; i++) {
                    if (counter / norm > percents[i]) {
                        if (!map.containsKey(names[i + 1])) {
                            map.put(names[i + 1], Value.of(chanel));
                        }
                    }
                }
            }
            for (String n : names) {
                if (!map.containsKey(n)) {
                    map.put(n, Value.of(lower));
                }
            }

            dataBuilder.row(new MapPoint(map));
        }
    }

}
