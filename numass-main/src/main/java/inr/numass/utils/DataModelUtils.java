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
package inr.numass.utils;

import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Values;
import inr.numass.data.SpectrumDataAdapter;

import java.util.Scanner;

/**
 *
 * @author Darksnake
 */
public class DataModelUtils {

    public static Table getUniformSpectrumConfiguration(double from, double to, double time, int numpoints) {
        assert to != from;
        final String[] list = {SpectrumDataAdapter.X_VALUE_KEY, "time"};
        ListTable.Builder res = new ListTable.Builder(list);

        for (int i = 0; i < numpoints; i++) {
            // формула работает даже в том случае когда порядок точек обратный
            double x = from + (to - from) / (numpoints - 1) * i;
            Values point = new ValueMap(list, x, time);
            res.row(point);
        }

        return res.build();
    }

    public static Table getSpectrumConfigurationFromResource(String resource) {
        final String[] list = {"x", "time"};
        ListTable.Builder res = new ListTable.Builder(list);
        Scanner scan = new Scanner(DataModelUtils.class.getResourceAsStream(resource));
        while (scan.hasNextLine()) {
            double x = scan.nextDouble();
            int time = scan.nextInt();
            res.row(new ValueMap(list, x, time));
        }
        return res.build();
    }

//    public static ListTable maskDataSet(Iterable<DataPoint> data, String maskForX, String maskForY, String maskForYerr, String maskForTime) {
//        ListTable res = new ListTable(XYDataPoint.names);
//        for (DataPoint point : data) {
//            res.addRow(SpectrumDataPoint.maskDataPoint(point, maskForX, maskForY, maskForYerr, maskForTime));
//        }
//        return res;
//    }    
}
