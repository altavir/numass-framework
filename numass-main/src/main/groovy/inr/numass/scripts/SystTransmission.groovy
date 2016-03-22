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
package inr.numass.scripts;

import hep.dataforge.context.GlobalContext;
import static hep.dataforge.context.GlobalContext.out;
import hep.dataforge.points.ListPointSet;
import hep.dataforge.datafitter.FitManager;
import hep.dataforge.datafitter.FitState;
import hep.dataforge.datafitter.FitTask;
import hep.dataforge.datafitter.MINUITPlugin

import hep.dataforge.datafitter.ParamSet;
import hep.dataforge.datafitter.models.XYModel;
import hep.dataforge.exceptions.NamingException;
import hep.dataforge.exceptions.PackFormatException;
import inr.numass.data.SpectrumDataAdapter;
import inr.numass.data.SpectrumGenerator;
import inr.numass.models.ModularTritiumSpectrum;
import inr.numass.models.NBkgSpectrum;
import inr.numass.models.ResolutionFunction
import inr.numass.utils.DataModelUtils;
import hep.dataforge.plotfit.PlotFitResultAction;
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.PlottableFunction
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import java.io.FileNotFoundException;
import java.util.Locale;
import org.apache.commons.math3.analysis.BivariateFunction

import static java.util.Locale.setDefault;

/**
 *
 * @author Darksnake
 */

setDefault(Locale.US);
new MINUITPlugin().startGlobal();

FitManager fm = new FitManager();

ResolutionFunction resolution = new ResolutionFunction(8.3e-5);
resolution.setTailFunction(ResolutionFunction.getRealTail());
ModularTritiumSpectrum beta = new ModularTritiumSpectrum(resolution, 18395d, 18580d, null);
beta.setCaching(false);

NBkgSpectrum spectrum = new NBkgSpectrum(beta);
XYModel model = new XYModel("tritium", spectrum, new SpectrumDataAdapter());

ParamSet allPars = new ParamSet();


allPars.setPar("N", 6e7, 1e5, 0, Double.POSITIVE_INFINITY);

allPars.setPar("bkg", 2, 0.1 );

allPars.setPar("E0", 18575.0, 0.1 );

allPars.setPar("mnu2", 0, 2);

def mster = 3000;// Mass of sterile neutrino in eV

allPars.setPar("msterile2", mster**2, 1);

allPars.setPar("U2", 0, 1e-4);

allPars.setPar("X", 0, 0.05, 0d, Double.POSITIVE_INFINITY);

allPars.setPar("trap", 1, 0.01, 0d, Double.POSITIVE_INFINITY);

int seed = 12316
SpectrumGenerator generator = new SpectrumGenerator(model, allPars, seed);

def config = DataModelUtils.getUniformSpectrumConfiguration(18400d, 18580, 1e7, 60)
//def config = DataModelUtils.getSpectrumConfigurationFromResource("/data/run23.cfg")

ListPointSet data = generator.generateExactData(config);

FitState state = new FitState(data, model, allPars);

println("Simulating data with real tail. Seed = ${seed}")

println("Fitting data with real parameters")

FitState res = fm.runTask(state, "QOW", FitTask.TASK_RUN, "N", "bkg","E0", "mnu2");
res.print(out());

def mnu2 = res.getParameters().getValue("mnu2");

println("Setting constant tail and fitting")
resolution.setTailFunction(ResolutionFunction.getConstantTail());

res = fm.runTask(state, "QOW", FitTask.TASK_RUN, "N", "bkg","E0", "mnu2");
res.print(out());

def diff = res.getParameters().getValue("mnu2") - mnu2;

println("\n\nSquared mass difference: ${diff}")