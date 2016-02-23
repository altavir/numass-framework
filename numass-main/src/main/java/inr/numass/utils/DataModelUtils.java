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

import hep.dataforge.data.DataPoint;
import hep.dataforge.data.ListPointSet;
import hep.dataforge.data.MapDataPoint;


/**
 *
 * @author Darksnake
 */
public class DataModelUtils {

    public static ListPointSet getUniformSpectrumConfiguration(double from, double to, double time, int numpoints) {
        assert to != from;
        final String[] list = {"x", "time"};
        ListPointSet res = new ListPointSet(list);

        for (int i = 0; i < numpoints; i++) {
            // формула работает даже в том случае когда порядок точек обратный
            double x = from + (to - from) / (numpoints - 1) * i;
            DataPoint point = new MapDataPoint(list, x,time);
            res.add(point);
        }

        return res;
    }
    
//    public static ListPointSet maskDataSet(Iterable<DataPoint> data, String maskForX, String maskForY, String maskForYerr, String maskForTime) {
//        ListPointSet res = new ListPointSet(XYDataPoint.names);
//        for (DataPoint point : data) {
//            res.add(SpectrumDataPoint.maskDataPoint(point, maskForX, maskForY, maskForYerr, maskForTime));
//        }
//        return res;
//    }    
}
