/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.maths;

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.values.Values;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 *
 * @author Alexander Nozik
 */
public class MathUtils {

    public static double[] getDoubleArray(Values set, String... names) {
        //fast access method for double vectors
        if (set instanceof NamedVector) {
            return ((NamedVector) set).getArray(names);
        }

        if (names.length == 0) {
            names = set.namesAsArray();
        }
        double[] res = new double[names.length];
        for (String name : names) {
            int index = set.getNames().getNumberByName(name);
            if (index < 0) {
                throw new NameNotFoundException(name);
            }
            res[index] = set.getDouble(name);
        }
        return res;
    }

    public static String toString(Values set, String... names) {
        String res = "[";
        if (names.length == 0) {
            names = set.getNames().asArray();
        }
        boolean flag = true;
        for (String name : names) {
            if (flag) {
                flag = false;
            } else {
                res += ", ";
            }
            res += name + ":" + set.getDouble(name);
        }
        return res + "]";
    }

    /**
     * calculate function on grid
     *
     * @param func
     * @param grid
     * @return
     */
    public static double[] calculateFunction(UnivariateFunction func, double[] grid) {
        double[] res = new double[grid.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = func.value(grid[i]);
        }
        return res;
    }

    /**
     * Calculate bivariate function on grid
     * @param func
     * @param xGrid
     * @param yGrid
     * @return 
     */
    public static double[][] calculateFunction(BivariateFunction func, double[] xGrid, double[] yGrid) {
        double[][] res = new double[xGrid.length][yGrid.length];
        for (int i = 0; i < xGrid.length; i++) {
            for (int j = 0; j < yGrid.length; j++) {
                res[i][j] = func.value(xGrid[i],yGrid[j]);
            }
        }
        return res;
    }
}
