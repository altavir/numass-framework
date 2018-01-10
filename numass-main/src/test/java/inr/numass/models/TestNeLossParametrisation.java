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
package inr.numass.models;

import hep.dataforge.maths.integration.GaussRuleIntegrator;
import hep.dataforge.plots.PlotFrame;
import hep.dataforge.plots.data.XYFunctionPlot;
import inr.numass.NumassPluginKt;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 *
 * @author Alexander Nozik
 */
@SuppressWarnings("unchecked")
public class TestNeLossParametrisation {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PlotFrame frame = NumassPluginKt.displayJFreeChart("Loss parametrisation test");
    //JFreeChartFrame.drawFrame("Loss parametrisation test", null);
        UnivariateFunction oldFunction = LossCalculator.getSingleScatterFunction();
        UnivariateFunction newFunction = getSingleScatterFunction(12.86, 16.78, 1.65, 12.38, 4.79);

        Double norm = new GaussRuleIntegrator(200).integrate(0d, 100d, newFunction);

        System.out.println(norm);

        frame.add(XYFunctionPlot.plotFunction("old", 0, 30, 300, oldFunction::value));
        frame.add(XYFunctionPlot.plotFunction("new", 0, 30, 300, newFunction::value));
    }

    public static UnivariateFunction getSingleScatterFunction(
            final double exPos,
            final double ionPos,
            final double exW,
            final double ionW,
            final double exIonRatio) {

        return (double eps) -> {
            if (eps <= 0) {
                return 0;
            }
            double z = eps - exPos;
            // Используется полная ширина, а не полуширина.
            double res = exIonRatio * Math.exp(-2 * z * z / exW / exW) * Math.sqrt(2 / Math.PI) / exW;

            if (eps >= ionPos) {
                z = 4 * (eps - ionPos) * (eps - ionPos);
                res += 4d / (1 + z / ionW / ionW) / Math.PI / ionW;
            }
            return res / (1 + exIonRatio);
        };
    }
}
