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

import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import inr.numass.data.SpectrumDataAdapter;
import inr.numass.storage.NMPoint;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;

/**
 * @author Darksnake
 */
public class TritiumUtils {

    public static Table correctForDeadTime(ListTable data, double dtime) {
        return correctForDeadTime(data, adapter(), dtime);
    }

    /**
     * Коррекция на мертвое время в секундах
     *
     * @param data
     * @param dtime
     * @return
     */
    public static Table correctForDeadTime(ListTable data, SpectrumDataAdapter adapter, double dtime) {
//        SpectrumDataAdapter adapter = adapter();
        ListTable.Builder res = new ListTable.Builder(data.getFormat());
        for (DataPoint dp : data) {
            double corrFactor = 1 / (1 - dtime * adapter.getCount(dp) / adapter.getTime(dp));
            res.row(adapter.buildSpectrumDataPoint(adapter.getX(dp).doubleValue(), (long) (adapter.getCount(dp) * corrFactor), adapter.getTime(dp)));
        }
        return res.build();
    }

    /**
     * Поправка масштаба высокого.
     *
     * @param data
     * @param beta
     * @return
     */
    public static Table setHVScale(ListTable data, double beta) {
        SpectrumDataAdapter reader = adapter();
        ListTable.Builder res = new ListTable.Builder(data.getFormat());
        for (DataPoint dp : data) {
            double corrFactor = 1 + beta;
            res.row(reader.buildSpectrumDataPoint(reader.getX(dp).doubleValue() * corrFactor, reader.getCount(dp), reader.getTime(dp)));
        }
        return res.build();
    }

    public static SpectrumDataAdapter adapter() {
        return new SpectrumDataAdapter("Uset", "CR", "CRerr", "Time");
    }

    /**
     * Integral beta spectrum background with given amplitude (total count rate
     * from)
     *
     * @param amplitude
     * @return
     */
    public static UnivariateFunction tritiumBackgroundFunction(double amplitude) {

        return (e) -> {
            /*чистый бета-спектр*/
            double e0 = 18575d;
            double D = e0 - e;//E0-E
            if (D <= 0) {
                return 0;
            }
            return amplitude * factor(e) * D * D;
        };
    }

    private static double factor(double E) {
        double me = 0.511006E6;
        double Etot = E + me;
        double pe = sqrt(E * (E + 2d * me));
        double ve = pe / Etot;
        double yfactor = 2d * 2d * 1d / 137.039 * Math.PI;
        double y = yfactor / ve;
        double Fn = y / abs(1d - exp(-y));
        double Fermi = Fn * (1.002037 - 0.001427 * ve);
        double res = Fermi * pe * Etot;
        return res * 1E-23;
    }

    public static double countRateWithDeadTime(NMPoint p, int from, int to, double deadTime) {
        double wind = p.getCountInWindow(from, to) / p.getLength();
        double res;
        if (deadTime > 0) {
            double total = p.getEventsCount();
            double time = p.getLength();
            res = wind / (1 - total * deadTime / time);
//            double timeRatio = deadTime / p.getLength();
//            res = wind / total * (1d - Math.sqrt(1d - 4d * total * timeRatio)) / 2d / timeRatio;
        } else {
            res = wind;
        }
        return res;
    }

    public static double countRateWithDeadTimeErr(NMPoint p, int from, int to, double deadTime) {
        return Math.sqrt(countRateWithDeadTime(p, from, to, deadTime) / p.getLength());
    }

    /**
     * Evaluate groovy expression using numass point as parameter
     *
     * @param point
     * @param expression
     * @return
     */
    public static double evaluateExpression(NMPoint point, String expression) {
        Map<String, Object> exprParams = new HashMap<>();
        exprParams.put("T", point.getLength());
        exprParams.put("U", point.getUread());
        exprParams.put("point", point);
        return ExpressionUtils.evaluate(expression, exprParams);
    }
}
