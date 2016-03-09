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

import hep.dataforge.points.DataPoint;
import hep.dataforge.points.ListPointSet;
import inr.numass.data.SpectrumDataAdapter;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import org.apache.commons.math3.analysis.UnivariateFunction;
import static java.lang.Math.abs;
import static java.lang.Math.abs;
import static java.lang.Math.abs;
import static java.lang.Math.abs;
import static java.lang.Math.abs;
import static java.lang.Math.abs;
import static java.lang.Math.abs;

/**
 *
 * @author Darksnake
 */

public class TritiumUtils {

//    /**
//     * Линейное уплывание интенсивности в зависимости от времени. Размерность:
//     * обратные секунды
//     *
//     * @param data
//     * @param driftPerSecond
//     * @return
//     */
//    public static ListPointSet applyDrift(ListPointSet data, double driftPerSecond) {
//        double t = 0;
//        
//        ListPointSet res = new ListPointSet(data.getDataFormat());
//        for (DataPoint d : data) {
//            SpectrumDataPoint dp = (SpectrumDataPoint) d;
//            double corrFactor = 1 + driftPerSecond * t;
//            dp = new SpectrumDataPoint(dp.getX(), (long) (dp.getCount() * corrFactor), dp.getTime());
//            res.add(dp);
//            t += dp.getTime();
//        }
//        return res;
//        
//    }

    /**
     * Коррекция на мертвое время в секундах
     *
     * @param data
     * @param dtime
     * @return
     */
    public static ListPointSet correctForDeadTime(ListPointSet data, double dtime) {
        SpectrumDataAdapter reader = new SpectrumDataAdapter(data.meta().getNode("aliases"));            
        ListPointSet res = new ListPointSet(data.getDataFormat());
        for (DataPoint dp : data) {
            double corrFactor = 1 / (1 - dtime * reader.getCount(dp) /reader.getTime(dp));
            res.add(reader.buildSpectrumDataPoint(reader.getX(dp).doubleValue(), (long) (reader.getCount(dp)*corrFactor),reader.getTime(dp)));
        }
        return res;
    }


    /**
     * Поправка масштаба высокого.
     *
     * @param data
     * @param beta
     * @return
     */
    public static ListPointSet setHVScale(ListPointSet data, double beta) {
        SpectrumDataAdapter reader = new SpectrumDataAdapter(data.meta().getNode("aliases"));        
        ListPointSet res = new ListPointSet(data.getDataFormat());
        for (DataPoint dp : data) {
            double corrFactor = 1 + beta;
            res.add(reader.buildSpectrumDataPoint(reader.getX(dp).doubleValue()*corrFactor, reader.getCount(dp), reader.getTime(dp)));
        }
        return res;
    }
    
    /**
     * Integral beta spectrum background with given amplitude (total count rate
     * from)
     *
     * @param energy
     * @param countRate
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
}
