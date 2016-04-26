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

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.XYAdapter;
import hep.dataforge.datafitter.FitState;
import hep.dataforge.datafitter.FitTaskResult;
import hep.dataforge.datafitter.Param;
import hep.dataforge.datafitter.ParamSet;
import hep.dataforge.datafitter.models.Histogram;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.PrintFunction;
import hep.dataforge.io.log.Logable;
import hep.dataforge.maths.GridCalculator;
import hep.dataforge.maths.NamedDoubleSet;
import hep.dataforge.maths.NamedMatrix;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.PlotsPlugin;
import hep.dataforge.plots.XYPlotFrame;
import hep.dataforge.plots.data.PlottableData;
import hep.dataforge.plots.data.PlottableFunction;
import hep.dataforge.simulation.GaussianParameterGenerator;
import inr.numass.NumassContext;
import inr.numass.models.ExperimentalVariableLossSpectrum;
import inr.numass.models.LossCalculator;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.LoggerFactory;
import hep.dataforge.tables.Table;

/**
 *
 * @author darksnake
 */
@TypedActionDef(name = "showLoss", inputType = FitState.class, outputType = FitState.class,
        description = "Show loss spectrum for fit with loss model. Calculate excitation to ionisation ratio.")
public class ShowLossSpectrumAction extends OneToOneAction<FitState, FitState> {

    private static final String[] names = {"X", "exPos", "ionPos", "exW", "ionW", "exIonRatio"};

    @Override
    protected FitState execute(Context context, Logable log, String name, Laminate meta, FitState input) {
        ParamSet pars = input.getParameters();
        if (!pars.names().contains(names)) {
            LoggerFactory.getLogger(getClass()).error("Wrong input FitState. Must be loss spectrum fit.");
            throw new RuntimeException("Wrong input FitState");
        }

        UnivariateFunction scatterFunction;
        boolean calculateRatio = false;
        XYPlotFrame frame = (XYPlotFrame) PlotsPlugin.buildFrom(context)
                .buildPlotFrame(getName(), name + ".loss",
                        new MetaBuilder("plot")
                        .setValue("plotTitle", "Differential scattering crossection for " + name)
                );
        switch (input.getModel().getName()) {
            case "scatter-variable":
                scatterFunction = LossCalculator.getSingleScatterFunction(pars);
                calculateRatio = true;

                LossCalculator.plotScatter(frame, pars);
                break;
            case "scatter-empiric-experimental":
                scatterFunction = new ExperimentalVariableLossSpectrum.Loss(0.3).total(pars);

                frame.add(new PlottableFunction("Cross-section", scatterFunction, 0, 100, 1000));
                break;
            default:
                throw new RuntimeException("Can work only with variable loss spectra");
        }

        double threshold = 0;
        double ionRatio = -1;
        double ionRatioError = -1;
        if (calculateRatio) {
            threshold = meta.getDouble("ionThreshold", 17);
            ionRatio = calcultateIonRatio(pars, threshold);
            log.log("The ionization ratio (using threshold {}) is {}", threshold, ionRatio);
            ionRatioError = calultateIonRatioError(context, name, input, threshold);
            log.log("the ionization ration standard deviation (using threshold {}) is {}", threshold, ionRatioError);
        }

        if (meta.getBoolean("printResult", false)) {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(buildActionOutput(context, name), Charset.forName("UTF-8")));
//            writer.println("*** FIT PARAMETERS ***");
            input.print(writer);
//            for (Param param : pars.getSubSet(names).getParams()) {
//                writer.println(param.toString());
//            }
//            writer.println();
//            out.printf("Chi squared over degrees of freedom: %g/%d = %g", input.getChi2(), input.ndf(), chi2 / this.ndf());

            writer.println();

            writer.println("*** LOSS SPECTRUM INFORMATION ***");
            writer.println();

            if (calculateRatio) {
                writer.printf("The ionization ratio (using threshold %f) is %f%n", threshold, ionRatio);
                writer.printf("The ionization ratio standard deviation (using threshold %f) is %f%n", threshold, ionRatioError);
                writer.println();
            }

//            double integralThreshold = reader.getDouble("numass.eGun", 19005d) - reader.getDouble("integralThreshold", 14.82);
//            double integralRatio = calculateIntegralExIonRatio(input.getDataSet(), input.getParameters().getValue("X"), integralThreshold);
//            writer.printf("The excitation to ionization ratio from integral spectrum (using threshold %f) is %f%n", integralThreshold, integralRatio);
            writer.println();

            writer.println("*** SUMMARY ***");

            writer.printf("%s\t", "name");

            for (String parName : names) {
                writer.printf("%s\t%s\t", parName, parName + "_err");
            }
            if (calculateRatio) {
                writer.printf("%s\t", "ionRatio");
                writer.printf("%s\t", "ionRatioErr");
            }
            writer.printf("%s%n", "chi2");

            writer.printf("%s\t", name);

            for (Param param : pars.getSubSet(names).getParams()) {
                writer.printf("%f\t%f\t", param.value(), param.getErr());
            }

            if (calculateRatio) {
                writer.printf("%f\t", ionRatio);
                writer.printf("%f\t", ionRatioError);
            }

            writer.printf("%f%n", input.getChi2() / ((FitTaskResult) input).ndf());
            writer.println();

            writer.println("***LOSS SPECTRUM***");
            writer.println();
            PrintFunction.printFunctionSimple(writer, scatterFunction, 0, 100, 500);

            if (meta.getBoolean("showSpread", false)) {
                writer.println("***SPECTRUM SPREAD***");
                writer.println();

                ParamSet parameters = input.getParameters().getSubSet(new String[]{"exPos", "ionPos", "exW", "ionW", "exIonRatio"});
                NamedMatrix covariance = input.getCovariance();
                Table spreadData = generateSpread(writer, name, parameters, covariance);
                ColumnedDataWriter.writeDataSet(System.out, spreadData, "", spreadData.getFormat().namesAsArray());
            }
        }

        return input;
    }

    public static double calcultateIonRatio(NamedDoubleSet set, double threshold) {
        UnivariateIntegrator integrator = NumassContext.highDensityIntegrator;
        UnivariateFunction integrand = LossCalculator.getSingleScatterFunction(set);
        return 1d - integrator.integrate(integrand, 5d, threshold);
    }

    private double calculateIntegralExIonRatio(Table data, double X, double integralThreshold) {
        double scatterProb = 1 - Math.exp(-X);

        double[] x = data.getColumn("Uset").asList().stream().mapToDouble((val) -> val.doubleValue()).toArray();
        double[] y = data.getColumn("CR").asList().stream().mapToDouble((val) -> val.doubleValue()).toArray();

        double yMax = StatUtils.max(y);

        UnivariateInterpolator interpolator = new LinearInterpolator();
        UnivariateFunction interpolated = interpolator.interpolate(x, y);

        double thresholdValue = interpolated.value(integralThreshold);

        double one = 1 - X * Math.exp(-X);

        double ionProb = (one - thresholdValue / yMax);
        double exProb = (thresholdValue / yMax - one + scatterProb);
        return exProb / ionProb;
    }

    public double calultateIonRatioError(Context context, String dataNeme, FitState state, double threshold) {
        ParamSet parameters = state.getParameters().getSubSet(new String[]{"exPos", "ionPos", "exW", "ionW", "exIonRatio"});
        NamedMatrix covariance = state.getCovariance();
        return calultateIonRatioError(context, dataNeme, parameters, covariance, threshold);
    }

    @SuppressWarnings("Unchecked")
    public double calultateIonRatioError(Context context, String name, NamedDoubleSet parameters, NamedMatrix covariance, double threshold) {
        int number = 10000;

        double[] res = new GaussianParameterGenerator(parameters, covariance)
                .generate(number)
                .stream()
                .mapToDouble((vector) -> calcultateIonRatio(vector, threshold))
                .filter(d -> !Double.isNaN(d))
                .toArray();

        Histogram hist = new Histogram(0.3, 0.5, 0.002);
        hist.fill(res);
        XYPlotFrame frame = (XYPlotFrame) PlotsPlugin.buildFrom(context)
                .buildPlotFrame(getName(), name + ".ionRatio",
                        new MetaBuilder("plot").setValue("plotTitle", "Ion ratio Distribution for " + name)
                );
//        XYPlotFrame frame = JFreeChartFrame.drawFrame("Ion ratio Distribution for " + name, null);
        frame.add(PlottableData.plot("ionRatio", hist, new XYAdapter("binCenter", "count")));

        return new DescriptiveStatistics(res).getStandardDeviation();
    }

    public static Table generateSpread(PrintWriter writer, String name, NamedDoubleSet parameters, NamedMatrix covariance) {
        int numCalls = 1000;
        int gridPoints = 200;
        double a = 8;
        double b = 32;

        double[] grid = GridCalculator.getUniformUnivariateGrid(a, b, gridPoints);

        double[] upper = new double[gridPoints];
        double[] lower = new double[gridPoints];
        double[] dispersion = new double[gridPoints];

        double[] central = new double[gridPoints];

        UnivariateFunction func = LossCalculator.getSingleScatterFunction(parameters);
        for (int j = 0; j < gridPoints; j++) {
            central[j] = func.value(grid[j]);
        }

        Arrays.fill(upper, Double.NEGATIVE_INFINITY);
        Arrays.fill(lower, Double.POSITIVE_INFINITY);
        Arrays.fill(dispersion, 0);

        GaussianParameterGenerator generator = new GaussianParameterGenerator(parameters, covariance);

        for (int i = 0; i < numCalls; i++) {
            func = LossCalculator.getSingleScatterFunction(generator.generate());
            for (int j = 0; j < gridPoints; j++) {
                double val = func.value(grid[j]);
                upper[j] = Math.max(upper[j], val);
                lower[j] = Math.min(lower[j], val);
                dispersion[j] += (val - central[j]) * (val - central[j]) / numCalls;
            }
        }
        String[] pointNames = {"e", "central", "lower", "upper", "dispersion"};
        ListTable.Builder res = new ListTable.Builder(pointNames);
        for (int i = 0; i < gridPoints; i++) {
            res.addRow(new MapPoint(pointNames, grid[i], central[i], lower[i], upper[i], dispersion[i]));

        }
        return res.build();
    }

}
