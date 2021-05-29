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
package hep.dataforge.maths;

import hep.dataforge.values.Values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>GridCalculator class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class GridCalculator {

    public static double[] getUniformUnivariateGrid(double a, double b, int numpoints) {
        assert b > a;
        assert numpoints > 1;
        double[] res = new double[numpoints];

        for (int i = 0; i < numpoints; i++) {
            res[i] = a + i * (b - a) / (numpoints - 1);
        }
        return res;
    }

    public static double[] getUniformUnivariateGrid(double a, double b, double step) {
        return getUniformUnivariateGrid(a, b, (int) ((b - a) / step +1));
    }
    
    public static List<Double> getFromData(Iterable<Values> data, String name){
        List<Double> grid = new ArrayList<>();
        for (Values point : data) {
            grid.add(point.getDouble(name));
        }
        return grid;
    }
    
    /**
     * Create grid with new node in each center of interval of the old grid
     * @param grid
     * @return 
     */
    public static List<Double> doubleGrid(List<Double> grid){
        Collections.sort(grid);
        List<Double> doubleGrid = new ArrayList<>();
        for (int i = 0; i < grid.size()-1; i++) {
            doubleGrid.add(grid.get(i));
            doubleGrid.add((grid.get(i)+grid.get(i+1))/2);            
        }
        doubleGrid.add(grid.get(grid.size())); 
        return doubleGrid;
    }

}
