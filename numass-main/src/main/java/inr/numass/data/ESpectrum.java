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
package inr.numass.data;

import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.tables.SimplePointSource;
import hep.dataforge.tables.TableFormatBuilder;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueType;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.lang.String.format;

/**
 *
 * @author Darksnake
 */
public class ESpectrum extends SimplePointSource {

    private final static String binCenter = "chanel";

    private static TableFormat prepareFormat(List<NMPoint> points) {
        TableFormatBuilder builder = new TableFormatBuilder();

        builder.addString(binCenter);
        points.stream().forEach((point) -> {
            builder.addColumn(format("%.3f", point.getUread()), 10, ValueType.NUMBER);
        });

        return builder.build();
    }

    int binning = 1;

    public ESpectrum(List<NMPoint> points, int binning, boolean normalize) {
        super(prepareFormat(points));
        this.binning = binning;
        fill(points, normalize);
    }

    private void fill(List<NMPoint> points, boolean normalize) {
        assert !points.isEmpty();

        List<Map<Double, Double>> spectra = new ArrayList<>();

        for (NMPoint numassPoint : points) {
            spectra.add(numassPoint.getMapWithBinning(binning, normalize));
        }

        for (Double x : spectra.get(0).keySet()) {
            Map<String, Value> res = new HashMap<>();
            res.put(binCenter, Value.of(x));
            for (int j = 0; j < points.size(); j++) {
                res.put(format("%.3f", points.get(j).getUread()), Value.of(spectra.get(j).get(x)));
            }
            this.addRow(new MapPoint(res));

        }
    }

    public void printToFile(OutputStream stream) {
        ColumnedDataWriter.writeDataSet(stream, this, null);
//        new ColumnedDataWriter(stream, this.getFormat().asArray()).writeDataSet(this, null);
    }

}
