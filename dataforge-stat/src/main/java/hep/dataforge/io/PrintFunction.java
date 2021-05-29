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
package hep.dataforge.io;

import hep.dataforge.maths.domains.Domain;
import hep.dataforge.maths.functions.MultiFunction;
import hep.dataforge.stat.UniformRandomVectorGenerator;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NullArgumentException;

import java.io.PrintWriter;
import java.util.List;

import static hep.dataforge.utils.ArgumentChecker.checkEqualDimensions;

/**
 * <p>PrintFunction class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class PrintFunction {

    /**
     * <p>printFunction.</p>
     *
     * @param out a {@link java.io.PrintWriter} object.
     * @param func a {@link org.apache.commons.math3.analysis.UnivariateFunction} object.
     * @param grid an array of double.
     * @param printIntegral a boolean.
     */
    public static void printFunction(PrintWriter out, UnivariateFunction func, double[] grid, boolean printIntegral) {
        if ((grid == null) || (func == null)) {
            throw new NullArgumentException();
        }
        double val;
        double valold;
        double integr = 0;
        int i;
        valold = func.value(grid[0]);
        if (printIntegral) {
            out.printf("%g\t%e\t%e%n", grid[0], valold, integr);
        } else {
            out.printf("%g\t%e%n", grid[0], valold);
        }
        for (i = 1; i < grid.length; i++) {
            val = func.value(grid[i]);
            integr += (grid[i] - grid[i - 1]) * (val + valold) / 2;
            if (printIntegral) {
                out.printf("%.8g\t%e\t%e%n", grid[i], val, integr);
            } else {
                out.printf("%.8g\t%e%n", grid[i], val);
            }
            valold = val;
            out.flush();
        }
        out.println();
        out.flush();
    }

    /**
     * <p>printFuntionSimple.</p>
     *
     * @param out a {@link java.io.PrintWriter} object.
     * @param func a {@link org.apache.commons.math3.analysis.UnivariateFunction} object.
     * @param a a double.
     * @param b a double.
     * @param numPoints a int.
     */
    public static void printFunctionSimple(PrintWriter out, UnivariateFunction func, double a, double b, int numPoints) {
        if (b <= a) {
            throw new IllegalArgumentException("Wrong boundaries for function printing");
        }
        double[] grid = new double[numPoints];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = a + (b - a) * i / (numPoints - 1);
        }
        printFunction(out, func, grid, false);
    }
    
    /**
     * Использует
     *
     * @param out a {@link java.io.PrintWriter} object.
     * @param func a {@link MultiFunction} object.
     * @param dom a {@link Domain} object.
     * @param numpoints - Количество вызовов функции
     */
    public static void printMultiFunction(PrintWriter out, MultiFunction func, Domain dom, int numpoints) {
        checkEqualDimensions(func.getDimension(),dom.getDimension());
        UniformRandomVectorGenerator generator = new UniformRandomVectorGenerator(dom);
        out.println();
        for (int i = 0; i < numpoints; i++) {
            double[] vector = generator.nextVector();
            double value = func.value(vector);

            for (double aVector : vector) {
                out.printf("%g\t", aVector);

            }
            out.printf("%g%n", value);
            
        }
        out.println();
        
    }
    
    /**
     * <p>printMultiFunction.</p>
     *
     * @param out a {@link java.io.PrintWriter} object.
     * @param head a {@link java.lang.String} object.
     * @param func a {@link MultiFunction} object.
     * @param points a {@link java.util.List} object.
     */
    public static void printMultiFunction(PrintWriter out, String head, MultiFunction func, List<double[]> points){
        out.printf("%n***%s***%n",head);
        for (double[] vector: points) {
            if(vector.length!=func.getDimension()){
                throw new DimensionMismatchException(vector.length, func.getDimension());
            }
            
            double value = func.value(vector);

            for (double aVector : vector) {
                out.printf("%g\t", aVector);

            }
            out.printf("%g%n", value);
            
        }
        out.println();    
    }
    
    /**
     * <p>printPoints.</p>
     *
     * @param out a {@link java.io.PrintWriter} object.
     * @param xval an array of double.
     * @param yval an array of double.
     * @param printIntegral a boolean.
     */
    public static void printPoints(PrintWriter out, double[] xval, double[] yval, boolean printIntegral){
        double[] integr;
        int i;
        if (printIntegral) {
            integr = new double[xval.length];
            integr[0] = 0;
            for (i = 1; i < xval.length; i++) {
                integr[i] = integr[i - 1] + (xval[i] - xval[i - 1]) * (yval[i] + yval[i - 1]) / 2;
            }
        } else {
            integr = null;
        }
        for (i = 0; i < xval.length; i++) {
            if (printIntegral) {
                out.printf("%g\t%g\t%g%n", xval[i], yval[i], integr[i]);
            } else {
                out.printf("%g\t%g%n", xval[i], yval[i]);
            }
        }
    }

}
