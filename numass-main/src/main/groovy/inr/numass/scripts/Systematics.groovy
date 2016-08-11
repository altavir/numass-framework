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

import hep.dataforge.stat.fit.*
import hep.dataforge.stat.models.XYModel
import hep.dataforge.tables.ListTable
import inr.numass.data.SpectrumDataAdapter
import inr.numass.data.SpectrumGenerator
import inr.numass.models.BetaSpectrum
import inr.numass.models.ModularSpectrum
import inr.numass.models.NBkgSpectrum
import inr.numass.models.ResolutionFunction
import inr.numass.utils.DataModelUtils
import org.apache.commons.math3.analysis.BivariateFunction

import static hep.dataforge.context.GlobalContext.out
import static java.util.Locale.setDefault

/**
 *
 * @author Darksnake
 */

setDefault(Locale.US);
new MINUITPlugin().startGlobal();

FitManager fm = new FitManager();

BivariateFunction resolution = new ResolutionFunction(8.3e-5);

ModularSpectrum beta = new ModularSpectrum(new BetaSpectrum(), resolution, 13490d, 18575d);
beta.setCaching(false);

NBkgSpectrum spectrum = new NBkgSpectrum(beta);
XYModel model = new XYModel("tritium", spectrum, new SpectrumDataAdapter());

ParamSet allPars = new ParamSet();


allPars.setPar("N", 6e5, 10, 0, Double.POSITIVE_INFINITY);

allPars.setPar("bkg", 2d, 0.1 );

allPars.setPar("E0", 18575.0, 0.05 );

allPars.setPar("mnu2", 0, 1);

def mster = 3000;// Mass of sterile neutrino in eV

allPars.setPar("msterile2", mster**2, 1);

allPars.setPar("U2", 0, 1e-4);

allPars.setPar("X", 0, 0.05, 0d, Double.POSITIVE_INFINITY);

allPars.setPar("trap", 0, 0.01, 0d, Double.POSITIVE_INFINITY);

SpectrumGenerator generator = new SpectrumGenerator(model, allPars, 12316);

ListTable data = generator.generateData(DataModelUtils.getUniformSpectrumConfiguration(14000d, 18200, 1e6, 60));

//        data = data.filter("X", Value.of(15510.0), Value.of(18610.0));
allPars.setParValue("U2", 0);
FitState state = new FitState(data, model, allPars);
//new PlotFitResultAction(GlobalContext.instance(), null).runOne(state);
        
//double delta = 4e-6;

//resolution.setTailFunction{double E, double U -> 
//    1-delta*(E-U);
//}

resolution.setTailFunction(ResolutionFunction.getRealTail())
 
//PlotFrame frame = JFreeChartFrame.drawFrame("Transmission function", null);
//frame.add(new PlottableFunction("transmission",null, {U -> resolution.value(18500,U)},13500,18505,500));

FitState res = fm.runTask(state, "QOW", FitTask.TASK_RUN, "N", "bkg", "E0", "U2", "trap");

        

res.print(out());

