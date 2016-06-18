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
package inr.numass.scripts


import hep.dataforge.datafitter.ParamSet
import hep.dataforge.maths.integration.RiemanIntegrator
import hep.dataforge.maths.integration.UnivariateIntegrator
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.PlottableXYFunction
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BisectionSolver
import inr.numass.models.LossCalculator
import inr.numass.models.ResolutionFunction
import inr.numass.NumassContext



ParamSet params = new ParamSet()
.setParValue("exPos", 12.76)
.setParValue("ionPos", 13.95)
.setParValue("exW", 1.2)
.setParValue("ionW", 13.5)
.setParValue("exIonRatio", 4.55)




UnivariateFunction scatterFunction = LossCalculator.getSingleScatterFunction(params);

PlotFrame frame = JFreeChartFrame.drawFrame("Differential scatter function", null);
frame.add(PlottableXYFunction.plotFunction("differential", scatterFunction, 0, 100, 400));

UnivariateIntegrator integrator = NumassContext.defaultIntegrator;

double border = 13.6;

UnivariateFunction ratioFunction = {e->integrator.integrate(scatterFunction, 0 , e) / integrator.integrate(scatterFunction, e, 100)}

double ratio = ratioFunction.value(border);
println "The true excitation to ionization ratio with border energy $border is $ratio";


double resolution = 1.5d;


def X = 0.527;

LossCalculator calculator = new LossCalculator();

List<Double> lossProbs = calculator.getGunLossProbabilities(X);

UnivariateFunction newScatterFunction = { double d -> 
    double res = scatterFunction.value(d);
    for(i = 1; i < lossProbs.size(); i++){
        res += lossProbs.get(i) * calculator.getLossValue(i, d, 0);
    }
    return res;
}


UnivariateFunction resolutionValue = {double e ->
    if (e <= 0d) {
        return 0d;
    } else if (e >= resolution) {
        return 1d;
    } else {
        return e/resolution;
    }
};


UnivariateFunction integral = {double u -> 
    if(u <= 0d){
        return 0d;
    } else {
        UnivariateFunction integrand = {double e -> resolutionValue.value(u-e) * newScatterFunction.value(e)};
        return integrator.integrate(integrand, 0d, u)
    }
}


frame.add(PlottableXYFunction.plotFunction("integral", integral, 0, 100, 800));

BisectionSolver solver = new BisectionSolver(1e-3);

UnivariateFunction integralShifted = {u -> 
    def integr = integral.value(u);
    return integr/(1-integr) - ratio;
}

double integralBorder = solver.solve(400, integralShifted, 10d, 20d);

println "The integral border is $integralBorder";

double newBorder = 14.43
double integralValue = integral.value(newBorder);

double err = Math.abs(integralValue/(1-integralValue)/ratio - 1d)
    
println "The relative error ic case of using $newBorder instead of real one is $err";