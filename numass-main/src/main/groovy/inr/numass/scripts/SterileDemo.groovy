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
import hep.dataforge.tables.ListTable;
import hep.dataforge.fitting.FitManager;
import hep.dataforge.fitting.FitState;
import hep.dataforge.fitting.FitTask;
import hep.dataforge.fitting.MINUITPlugin

import hep.dataforge.fitting.ParamSet;
import hep.dataforge.fitting.models.XYModel;
import hep.dataforge.fitting.parametric.ParametricFunction
import hep.dataforge.exceptions.NamingException;
import hep.dataforge.exceptions.PackFormatException;
import inr.numass.data.SpectrumDataAdapter;
import inr.numass.data.SpectrumGenerator;
import inr.numass.models.BetaSpectrum
import inr.numass.models.ModularSpectrum;
import inr.numass.models.NBkgSpectrum;
import inr.numass.models.sterile.SterileNeutrinoSpectrum
import inr.numass.utils.DataModelUtils;
import hep.dataforge.plotfit.PlotFitResultAction;
import java.io.FileNotFoundException;
import java.util.Locale;
import static java.util.Locale.setDefault;
import inr.numass.utils.TritiumUtils;
import inr.numass.data.SpectrumDataAdapter;
import hep.dataforge.io.FittingIOUtils

/**
 *
 * @author Darksnake
 */

setDefault(Locale.US);

ModularSpectrum beta = new ModularSpectrum(new BetaSpectrum(), 8.3e-5, 13990d, 18600d);

//ParametricFunction beta = new SterileNeutrinoSpectrum();

NBkgSpectrum spectrum = new NBkgSpectrum(beta);
XYModel model = new XYModel(spectrum, new SpectrumDataAdapter());

ParamSet allPars = new ParamSet();

allPars.setPar("N", 6.6579e+05,  1.8e+03, 0d, Double.POSITIVE_INFINITY);
allPars.setPar("bkg", 0.5387, 0.050);
allPars.setPar("E0", 18574.94, 1.4);
allPars.setPar("mnu2", 0d, 1d);
allPars.setPar("msterile2", 1000d * 1000d,0);
allPars.setPar("U2", 0.0, 1e-4, -1d, 1d);
allPars.setPar("X", 0.04000, 0.01, 0d, Double.POSITIVE_INFINITY);
allPars.setPar("trap", 1.634, 0.01,0d, Double.POSITIVE_INFINITY);

FittingIOUtils.printSpectrum(GlobalContext.out(), spectrum, allPars, 14000.0, 18600.0, 400);

//SpectrumGenerator generator = new SpectrumGenerator(model, allPars, 12316);
//
//ListTable data = generator.generateData(DataModelUtils.getUniformSpectrumConfiguration(14000d, 18500, 2000, 90));
//
//data = TritiumUtils.correctForDeadTime(data, new SpectrumDataAdapter(), 1e-8);
////        data = data.filter("X", Value.of(15510.0), Value.of(18610.0));
////        allPars.setParValue("X", 0.4);
//FitState state = new FitState(data, model, allPars);
////new PlotFitResultAction().eval(state);
//        
//        
//FitState res = fm.runTask(state, "QOW", FitTask.TASK_RUN, "N", "bkg", "E0", "U2", "trap");
//
//        
//
//res.print(out());
//
