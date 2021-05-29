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
package hep.dataforge.MINUIT;

import org.apache.commons.math3.linear.RealVector;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utilities for printing various minuit results.
 *
 * @version $Id$
 * @author Darksnake
 */
public abstract class MnPrint {

    /**
     * <p>print.</p>
     *
     * @param os a {@link java.io.PrintWriter} object.
     * @param vec a {@link org.apache.commons.math3.linear.RealVector} object.
     */
    public static void print(PrintWriter os, RealVector vec) {
        os.println("LAVector parameters:");
        {
            os.println();
            int nrow = vec.getDimension();
            for (int i = 0; i < nrow; i++) {
                os.printf("%g ", vec.getEntry(i));
            }
            os.println();
        }
    }

    /**
     * <p>print.</p>
     *
     * @param os a {@link java.io.PrintWriter} object.
     * @param matrix a {@link hep.dataforge.MINUIT.MnAlgebraicSymMatrix} object.
     */
    public static void print(PrintWriter os, MnAlgebraicSymMatrix matrix) {
        os.println("LASymMatrix parameters:");
        {
            os.println();
            int n = matrix.nrow();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    os.printf("%10g ", matrix.get(i, j));
                }
                os.println();
            }
        }
    }

    /**
     * <p>print.</p>
     *
     * @param os a {@link java.io.PrintWriter} object.
     * @param min a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     */
    public static void print(PrintWriter os, FunctionMinimum min) {

        os.println();
        if (!min.isValid()) {
            os.println();
            os.println("WARNING: Minuit did not converge.");
            os.println();
        } else {
            os.println();
            os.println("Minuit did successfully converge.");
            os.println();
        }

        os.printf("# of function calls: %d\n", min.nfcn());
        os.printf("minimum function value: %g\n", min.fval());
        os.printf("minimum edm: %g\n", min.edm());
        os.println("minimum internal state vector: " + min.parameters().vec());
        if (min.hasValidCovariance()) {
            os.println("minimum internal covariance matrix: " + min.error().matrix());
        }

        os.println(min.userParameters());
        os.println(min.userCovariance());
        os.println(min.userState().globalCC());

        if (!min.isValid()) {
            os.println("WARNING: FunctionMinimum is invalid.");
        }

        os.println();
    }

    /**
     * <p>print.</p>
     *
     * @param os a {@link java.io.PrintWriter} object.
     * @param min a {@link hep.dataforge.MINUIT.MinimumState} object.
     */
    public static void print(PrintWriter os, MinimumState min) {
        os.println();
        
        os.printf("minimum function value: %g\n", min.fval());
        os.printf("minimum edm: %g\n", min.edm());
        os.println("minimum internal state vector: " + min.vec());
        os.println("minimum internal gradient vector: " + min.gradient().getGradient());
        if (min.hasCovariance()) {
            os.println("minimum internal covariance matrix: " + min.error().matrix());
        }

        os.println();
    }

    /**
     * <p>print.</p>
     *
     * @param os a {@link java.io.PrintWriter} object.
     * @param par a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     */
    public static void print(PrintWriter os, MnUserParameters par) {
        
        os.println();
        
        os.println("# ext. |" + "|   name    |" + "|   type  |" + "|   value   |" + "|  error +/- ");
        
        os.println();
        
        boolean atLoLim = false;
        boolean atHiLim = false;
        for (MinuitParameter ipar : par.parameters()) {
            os.printf(" %5d || %9s || ", ipar.number(), ipar.name());
            if (ipar.isConst()) {
                os.printf("         || %10g   ||", ipar.value());
            } else if (ipar.isFixed()) {
                os.printf("  fixed  || %10g   ||\n", ipar.value());
            } else if (ipar.hasLimits()) {
                if (ipar.error() > 0.) {
                    os.printf(" limited || %10g", ipar.value());
                    if (Math.abs(ipar.value() - ipar.lowerLimit()) < par.precision().eps2()) {
                        os.print("* ");
                        atLoLim = true;
                    }
                    if (Math.abs(ipar.value() - ipar.upperLimit()) < par.precision().eps2()) {
                        os.print("**");
                        atHiLim = true;
                    }
                    os.printf(" || %10g\n", ipar.error());
                } else {
                    os.printf("  free   || %10g || no\n", ipar.value());
                }
            } else {
                if (ipar.error() > 0.) {
                    os.printf("  free   || %10g || %10g\n", ipar.value(), ipar.error());
                } else {
                    os.printf("  free   || %10g || no\n", ipar.value());
                }
                
            }
        }
        os.println();
        if (atLoLim) {
            os.print("* parameter is at lower limit");
        }
        if (atHiLim) {
            os.print("** parameter is at upper limit");
        }
        os.println();
    }

    /**
     * <p>print.</p>
     *
     * @param os a {@link java.io.PrintWriter} object.
     * @param matrix a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     */
    public static void print(PrintWriter os, MnUserCovariance matrix) {

        os.println();

        os.println("MnUserCovariance: ");

        {
            os.println();
            int n = matrix.nrow();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    os.printf("%10g ", matrix.get(i, j));
                }
                os.println();
            }
        }

        os.println();
        os.println("MnUserCovariance parameter correlations: ");

        {
            os.println();
            int n = matrix.nrow();
            for (int i = 0; i < n; i++) {
                double di = matrix.get(i, i);
                for (int j = 0; j < n; j++) {
                    double dj = matrix.get(j, j);
                    os.printf("%g ", matrix.get(i, j) / Math.sqrt(Math.abs(di * dj)));

                }
                os.println();
            }
        }
    }

    /**
     * <p>print.</p>
     *
     * @param os a {@link java.io.PrintWriter} object.
     * @param coeff a {@link hep.dataforge.MINUIT.MnGlobalCorrelationCoeff} object.
     */
    public static void print(PrintWriter os, MnGlobalCorrelationCoeff coeff) {

        os.println();

        os.println("MnGlobalCorrelationCoeff: ");

        {
            os.println();
            for (int i = 0; i < coeff.globalCC().length; i++) {
                os.printf("%g\n", coeff.globalCC()[i]);

            }
        }
    }

    /**
    * <p>print.</p>
    *
    * @param os a {@link java.io.PrintWriter} object.
    * @param state a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
    */
   public static void print(PrintWriter os, MnUserParameterState state) {
       
       os.println();
       
       if (!state.isValid()) {
           os.println();
           os.println("WARNING: MnUserParameterState is not valid.");
           os.println();
       }
       
       os.println("# of function calls: " + state.nfcn());
       os.println("function value: " + state.fval());
       os.println("expected distance to the minimum (edm): " + state.edm());
       os.println("external parameters: " + state.parameters());
       if (state.hasCovariance()) {
           os.println("covariance matrix: " + state.covariance());
       }
       if (state.hasGlobalCC()) {
           os.println("global correlation coefficients : " + state.globalCC());
       }
       
       if (!state.isValid()) {
           os.println("WARNING: MnUserParameterState is not valid.");
       }
       
       os.println();
   }

    /**
     * <p>print.</p>
     *
     * @param os a {@link java.io.PrintWriter} object.
     * @param me a {@link hep.dataforge.MINUIT.MinosError} object.
     */
    public static void print(PrintWriter os, MinosError me) {
        os.println();
        os.printf("Minos # of function calls: %d\n", me.nfcn());

        if (!me.isValid()) {
            os.println("Minos error is not valid.");
        }
        if (!me.lowerValid()) {
            os.println("lower Minos error is not valid.");
        }
        if (!me.upperValid()) {
            os.println("upper Minos error is not valid.");
        }
        if (me.atLowerLimit()) {
            os.println("Minos error is lower limit of parameter " + me.parameter());
        }
        if (me.atUpperLimit()) {
            os.println("Minos error is upper limit of parameter " + me.parameter());
        }
        if (me.atLowerMaxFcn()) {
            os.println("Minos number of function calls for lower error exhausted.");
        }
        if (me.atUpperMaxFcn()) {
            os.println("Minos number of function calls for upper error exhausted.");
        }
        if (me.lowerNewMin()) {
            os.println("Minos found a new minimum in negative direction.");
            os.println(me.lowerState());
        }
        if (me.upperNewMin()) {
            os.println("Minos found a new minimum in positive direction.");
            os.println(me.upperState());
        }

        os.println("# ext. ||   name    || value@min ||  negative || positive  ");
        os.printf("%4d||%10s||%10g||%10g||%10g\n", me.parameter(), me.lowerState().name(me.parameter()), me.min(), me.lower(), me.upper());
        os.println();
    }

    /**
    * <p>print.</p>
    *
    * @param os a {@link java.io.PrintWriter} object.
    * @param ce a {@link hep.dataforge.MINUIT.ContoursError} object.
    */
   public static void print(PrintWriter os, ContoursError ce) {
       os.println();
       os.println("Contours # of function calls: " + ce.nfcn());
       os.println("MinosError in x: ");
       os.println(ce.xMinosError());
       os.println("MinosError in y: ");
       os.println(ce.yMinosError());
       MnPlot plot = new MnPlot();
       plot.plot(ce.xmin(), ce.ymin(), ce.points());
       int i = 0;
       for (Range ipoint : ce.points()) {
           os.printf("%d %10g %10g\n", i++, ipoint.getFirst(), ipoint.getSecond());
       }
       os.println();
   }

    static String toString(RealVector x) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, x);
        }
        return writer.toString();
    }

    static String toString(MnAlgebraicSymMatrix x) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, x);
        }
        return writer.toString();
    }

    static String toString(FunctionMinimum min) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, min);
        }
        return writer.toString();
    }

    static String toString(MinimumState x) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, x);
        }
        return writer.toString();
    }

    static String toString(MnUserParameters x) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, x);
        }
        return writer.toString();
    }

    static String toString(MnUserCovariance x) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, x);
        }
        return writer.toString();
    }

    static String toString(MnGlobalCorrelationCoeff x) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, x);
        }
        return writer.toString();
    }

    static String toString(MnUserParameterState x) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, x);
        }
        return writer.toString();
    }

    static String toString(MinosError x) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, x);
        }
        return writer.toString();
    }

    static String toString(ContoursError x) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            MnPrint.print(pw, x);
        }
        return writer.toString();
    }

    private MnPrint() {
    }
}
