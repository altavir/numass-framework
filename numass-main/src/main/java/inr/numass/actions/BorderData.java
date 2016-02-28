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

import hep.dataforge.data.ListPointSet;
import hep.dataforge.data.MapPoint;
import hep.dataforge.values.Value;
import inr.numass.data.NMFile;
import inr.numass.data.NMPoint;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Darksnake
 */
public class BorderData extends ListPointSet {

    private final static String[] names = {"U", "80%", "90%", "95%", "99%"};
    private final static double[] percents = {0.8, 0.9, 0.95, 0.99};

    public static double getNorm(Map<Double, Double> spectrum, int lower, int upper) {
        double res = 0;
        for (Map.Entry<Double, Double> entry : spectrum.entrySet()) {
            if ((entry.getKey() >= lower) && (entry.getKey() <= upper)) {
                res += entry.getValue();
            }
        }

        return res;
    }

    public BorderData(NMFile file, int upper, int lower, NMPoint reference) {
        super(names);
        if (upper <= lower) {
            throw new IllegalArgumentException();
        }
        fill(file, lower, upper, reference);
    }

    private void fill(NMFile file, int lower, int upper, NMPoint reference) {
        for (NMPoint point : file.getNMPoints()) {
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
            double norm = getNorm(spectrum, lower, upper);
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
            this.add(new MapPoint(map));
        }
    }

}
