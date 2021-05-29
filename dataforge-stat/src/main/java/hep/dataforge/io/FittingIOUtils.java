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

import hep.dataforge.maths.GridCalculator;
import hep.dataforge.maths.NamedMatrix;
import hep.dataforge.maths.NamedVector;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.stat.fit.Param;
import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.stat.likelihood.LogLikelihood;
import hep.dataforge.stat.models.Model;
import hep.dataforge.stat.models.XYModel;
import hep.dataforge.stat.parametric.ParametricFunction;
import hep.dataforge.stat.parametric.ParametricUtils;
import hep.dataforge.stat.parametric.ParametricValue;
import hep.dataforge.tables.Adapters;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.ValuesAdapter;
import hep.dataforge.values.ValueMap;
import hep.dataforge.values.Values;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Logger;

import static hep.dataforge.io.PrintFunction.printFunctionSimple;
import static hep.dataforge.names.NamesUtils.combineNames;
import static java.lang.Math.*;
import static java.util.Locale.setDefault;

/**
 * Some IOUtils for fitting module
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class FittingIOUtils {

    public static Table getNamedFunctionData(ParametricValue func, List<NamedVector> points) {
        final String[] format = combineNames(func.namesAsArray(), "value");
        ListTable.Builder res = new ListTable.Builder(format);
        Double[] values = new Double[func.getNames().size() + 1];
        for (NamedVector point : points) {
            for (int j = 0; j < func.getNames().size(); j++) {
                values[j] = point.getVector().getEntry(j);
            }
            values[values.length - 1] = func.value(point);
            Values dp = ValueMap.Companion.of(format, (Object[]) values);
            res.row(dp);
        }
        return res.build();
    }

    public static Values getValueSet(String names, String doubles) {
        Logger.getAnonymousLogger().warning("Using obsolete input method.");
        setDefault(Locale.ENGLISH);
        int i;
        try (Scanner sc = new Scanner(names)) {
            i = 0;
            sc.useDelimiter("\\p{Space}");
            while (sc.hasNext()) {
                sc.next();
                i++;
            }
        }
        String[] list = new String[i];
        double[] values = new double[i];
        Scanner sc1 = new Scanner(names);
        Scanner sc2 = new Scanner(doubles);
        sc1.useDelimiter("\\p{Space}");


        for (i = 0; i < list.length; i++) {
            list[i] = sc1.next();
            if (!sc2.hasNextDouble()) {
                throw new RuntimeException("Wrong input for ParamSet.");
            }
            values[i] = sc2.nextDouble();
        }
        return new NamedVector(list, values);
    }

    public static void printParamSet(PrintWriter out, ParamSet set) {
        out.println();

        set.getParams().forEach((param) -> {
            out.println(param.toString());
        });
    }

    public static ParamSet scanParamSet(Iterator<String> reader) {
        String line = reader.next();
        Scanner scan;
        String str;

        Param par;
        ArrayList<Param> pars = new ArrayList<>();
        String name;
        double value;
        double err;
//        Double lower;
//        Double upper;

        Double lowerBound;
        Double upperBound;

        if (!line.startsWith("{")) {
            throw new RuntimeException("Syntax error. Line should begin with \'{\' ");
        }
        line = reader.next();
        while (!line.startsWith("}")) {
            scan = new Scanner(line);
            //           str = scan.next("*\t:");
            str = scan.findInLine("^.*:");
            if (str == null) {
                throw new RuntimeException("Syntax error. Wrong format for parameter definition.");
            }
            name = str.substring(str.indexOf('\'') + 1, str.lastIndexOf('\''));
            par = new Param(name);
            value = scan.nextDouble();
            par.setValue(value);

            if (scan.hasNextDouble()) {
                err = scan.nextDouble();
                par.setErr(err);
                if (scan.hasNextDouble()) {
                    lowerBound = scan.nextDouble();
                    upperBound = scan.nextDouble();
                } else {
                    lowerBound = Double.NEGATIVE_INFINITY;
                    upperBound = Double.POSITIVE_INFINITY;
                }
                par.setDomain(lowerBound, upperBound);
            }


            pars.add(par);
            line = reader.next();
        }

        int i;
        ParamSet res = new ParamSet();
        for (i = 0; i < pars.size(); i++) {
            res.setPar(pars.get(i));
        }

        return res;

    }

    /**
     * Выводит на печать значения прадвоподобия (с автоматическим
     * масштабированием) по двум параметрам. Сначала идет перебор по параметру
     * {@code par1}, потом по {@code par2}.
     *
     * @param out   a {@link java.io.PrintWriter} object.
     * @param head  a {@link java.lang.String} object.
     * @param res   a {@link hep.dataforge.stat.fit.FitState} object.
     * @param par1  a {@link java.lang.String} object.
     * @param par2  a {@link java.lang.String} object.
     * @param num1  a int.
     * @param num2  a int.
     * @param scale - на сколько ошибоку нужно отступать от максимума
     */
    public static void printLike2D(OutputStream out, String head, FitState res, String par1, String par2, int num1, int num2, double scale) {

        double val1 = res.getParameters().getDouble(par1);
        double val2 = res.getParameters().getDouble(par2);
        double err1 = res.getParameters().getError(par1);
        double err2 = res.getParameters().getError(par2);

        double[] grid1 = GridCalculator.getUniformUnivariateGrid(val1 - scale * err1, val1 + scale * err1, num1);
        double[] grid2 = GridCalculator.getUniformUnivariateGrid(val2 - scale * err2, val2 + scale * err2, num2);

        LogLikelihood like = res.getLogLike();
        ParametricValue func = ParametricUtils.getNamedSubFunction(like.getLikelihood(), res.getParameters(), par1, par2);

        double[] vector = new double[2];

        String[] names = {par1, par2};

        ArrayList<NamedVector> points = new ArrayList<>();

        for (double x : grid1) {
            vector[0] = x;
            for (double y : grid2) {
                vector[1] = y;
                points.add(new NamedVector(names, vector));
            }
        }

        Table data = getNamedFunctionData(func, points);

        ColumnedDataWriter.writeTable(out, data, head);
    }

    public static void printLogProb1D(PrintWriter out, FitState res, int numpoints, double scale, String name) {
        LogLikelihood like = res.getLogLike();
        UnivariateFunction func = ParametricUtils.getNamedProjection(like, name, res.getParameters());
        Param p = res.getParameters().getByName(name);
        double a = max(p.getValue() - scale * p.getErr(), p.getLowerBound());
        double b = min(p.getValue() + scale * p.getErr(), p.getUpperBound());
        printFunctionSimple(out, func, a, b, numpoints);
    }

    /**
     * Использует информацию об ошибках для определения региона. И случайный
     * гауссовский генератор
     *
     * @param out       a {@link java.io.PrintWriter} object.
     * @param head      a {@link java.lang.String} object.
     * @param res       a {@link hep.dataforge.stat.fit.FitState} object.
     * @param numpoints a int.
     * @param scale     a double.
     * @param names     a {@link java.lang.String} object.
     */
    public static void printLogProbRandom(OutputStream out, String head, FitState res, int numpoints, double scale, String... names) {

        assert names.length > 0;
        LogLikelihood like = res.getLogLike();
        ParametricValue func = ParametricUtils.getNamedSubFunction(like, res.getParameters(), names);

        double[] vals = res.getParameters().getParValues(names).getArray();

        NamedMatrix fullCov = res.getCovariance();
        RealMatrix reducedCov = fullCov.subMatrix(names).getMatrix().scalarMultiply(scale);
        MultivariateNormalDistribution distr
                = new MultivariateNormalDistribution(vals, reducedCov.getData());

        ArrayList<NamedVector> points = new ArrayList<>();

        for (int i = 0; i < numpoints; i++) {
            points.add(new NamedVector(names, distr.sample()));
        }

        Table data = getNamedFunctionData(func, points);

        ColumnedDataWriter.writeTable(out, data, head);
    }

    public static void printNamedMatrix(PrintWriter out, NamedMatrix matrix) {
        out.println();

        String[] nameList = matrix.namesAsArray();
        for (String nameList1 : nameList) {
            out.printf("%-10s\t", nameList1);
        }

        out.println();
        out.println();

        for (int i = 0; i < nameList.length; i++) {
            for (int j = 0; j < nameList.length; j++) {
                out.printf("%10g\t", matrix.getMatrix().getEntry(i, j));

            }
            out.println();
        }

        out.println();
    }

    public static void printResiduals(PrintWriter out, FitState state) {
        printResiduals(out, state.getModel(), state.getData(), state.getParameters());
    }

    public static void printResiduals(PrintWriter out, Model model, Iterable<Values> data, ParamSet pars) {
        out.println();// можно тут вставить шапку
        out.printf("residual\tsigma%n%n");
        for (Values dp : data) {
            double dif = model.distance(dp, pars);
            double sigma = sqrt(model.dispersion(dp, pars));
            out.printf("%g\t%g%n", dif / sigma, sigma);
        }
        out.flush();
    }

    public static void printSpectrum(PrintWriter out, ParametricFunction sp, Values pars, double a, double b, int numPoints) {
        UnivariateFunction func = ParametricUtils.getSpectrumFunction(sp, pars);
        printFunctionSimple(out, func, a, b, numPoints);
        out.flush();
    }

    public static void printSpectrumResiduals(PrintWriter out, XYModel model, Iterable<Values> data, Values pars) {
        printSpectrumResiduals(out, model.getSpectrum(), data, model.getAdapter(), pars);
    }

    public static void printSpectrumResiduals(PrintWriter out, ParametricFunction spectrum,
                                              Iterable<Values> data, ValuesAdapter adapter, Values pars) {
        out.println();// можно тут вставить шапку
        out.printf("%8s\t%8s\t%8s\t%8s\t%8s%n", "x", "data", "error", "fit", "residual");

        for (Values dp : data) {
            double x = Adapters.getXValue(adapter, dp).getDouble();
            double y = Adapters.getYValue(adapter, dp).getDouble();
            double sigma = Adapters.getError(adapter, Adapters.Y_AXIS, dp).getDouble();

            double value = spectrum.value(x, pars);
            double dif = -(value - y) / sigma;

            out.printf("%8g\t%8g\t%8g\t%8g\t%8g%n", x, y, sigma, value, dif);
        }
        out.flush();
    }


}
