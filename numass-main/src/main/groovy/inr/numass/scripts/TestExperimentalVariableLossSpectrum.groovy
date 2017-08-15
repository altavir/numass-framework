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

import hep.dataforge.io.PrintFunction
import hep.dataforge.maths.integration.UnivariateIntegrator
import hep.dataforge.plots.data.PlottableXYFunction
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.stat.fit.ParamSet
import inr.numass.models.ExperimentalVariableLossSpectrum
import org.apache.commons.math3.analysis.UnivariateFunction

//double exPos = 12.94
//double exW = 1.31
//double ionPos = 14.13
//double ionW = 12.79
//double exIonRatio = 0.6059

ParamSet params = new ParamSet()
.setParValue("shift",0)
.setParValue("X", 0.4)
.setParValue("exPos", 12.94)
.setParValue("ionPos", 15.6)
.setParValue("exW", 1.31)
.setParValue("ionW", 12.79)
.setParValue("exIonRatio", 0.6059)

ExperimentalVariableLossSpectrum lsp = new ExperimentalVariableLossSpectrum(19005, 8e-5, 19010,0.2);


JFreeChartFrame frame = JFreeChartFrame.drawFrame("Experimental Loss Test", null);
UnivariateIntegrator integrator = NumassContext.defaultIntegrator

UnivariateFunction exFunc = lsp.excitation(params.getValue("exPos"), params.getValue("exW"));
frame.add(PlottableXYFunction.plotFunction("ex", exFunc, 0d, 50d, 500));

println "excitation norm factor " + integrator.integrate(0, 50, exFunc)

UnivariateFunction ionFunc = lsp.ionization(params.getValue("ionPos"), params.getValue("ionW"));
frame.add(PlottableXYFunction.plotFunction("ion",  ionFunc, 0d, 50d, 500));

println "ionization norm factor " + integrator.integrate(0, 200, ionFunc)

UnivariateFunction sumFunc = lsp.singleScatterFunction(params);
frame.add(PlottableXYFunction.plotFunction("sum",  sumFunc, 0d, 50d, 500));

println "sum norm factor " + integrator.integrate(0, 100, sumFunc)

PrintFunction.printFunctionSimple(new PrintWriter(System.out), sumFunc, 0d, 50d, 100)


JFreeChartFrame integerFrame = JFreeChartFrame.drawFrame("Experimental Loss Test", null);

UnivariateFunction integr = { d-> lsp.value(d,params)}
integerFrame.add(PlottableXYFunction.plotFunction("integr", integr, 18950d, 19005d, 500));