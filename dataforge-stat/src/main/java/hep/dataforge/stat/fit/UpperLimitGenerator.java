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

package hep.dataforge.stat.fit;

import hep.dataforge.io.ColumnedDataReader;
import hep.dataforge.tables.Column;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Value;
import kotlin.Pair;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

/**
 * An utility class to generate upper limit for parameters near physical boundary assuming parameter distribution is Gaussian
 * Created by darksnake on 16-Aug-16.
 */
public class UpperLimitGenerator {
    private static Table readFile() {
        return new ColumnedDataReader(UpperLimitGenerator.class.getResourceAsStream("/upperlimit.txt"), "val", "Bayes", "FC_low", "FC_high", "confidence").toTable();
    }

    /**
     * interpolate appropriate column
     *
     * @param val
     * @param columnName
     * @return
     */
    private static double interpolate(double val, String columnName) {
        Column x = readFile().getColumn("val");
        Column y = readFile().getColumn(columnName);

        double[] xArray = x.stream().mapToDouble(Value::getDouble).toArray();
        double[] yArray = y.stream().mapToDouble(Value::getDouble).toArray();

        SplineInterpolator interpolator = new SplineInterpolator();
        //PENDING add function caching here?
        return interpolator.interpolate(xArray, yArray).value(val);
    }

    public static double getBayesianUpperLimit(double val) {
        return interpolate(val, "Bayes");
    }

    public static double getConfidenceLimit(double val) {
        return interpolate(val, "confidence");
    }

    public static Pair<Double, Double> getFeldmanCousinsInterval(double val) {
        return new Pair<>(interpolate(val, "FC_low"), interpolate(val, "FC_high"));
    }
}
