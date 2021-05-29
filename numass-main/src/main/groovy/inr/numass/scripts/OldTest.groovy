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

import hep.dataforge.context.Global
import hep.dataforge.stat.fit.FitManager
import hep.dataforge.stat.fit.FitState
import hep.dataforge.stat.fit.MINUITPlugin
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.models.XYModel
import hep.dataforge.tables.ListTable
import inr.numass.data.SpectrumAdapter
import inr.numass.models.BetaSpectrum
import inr.numass.models.ModularSpectrum
import inr.numass.models.NBkgSpectrum
import inr.numass.models.ResolutionFunction
import org.apache.commons.math3.analysis.BivariateFunction

import static inr.numass.utils.OldDataReader.readData

PrintWriter out = Global.out();
Locale.setDefault(Locale.US);

new MINUITPlugin().startGlobal();

FitManager fm = new FitManager();

//        setSeed(543982);
File fssfile = new File("c:\\Users\\Darksnake\\Dropbox\\PlayGround\\FS.txt");

BivariateFunction resolution = new ResolutionFunction(2.28e-4);
//resolution.setTailFunction(ResolutionFunction.getRealTail())

ModularSpectrum sp = new ModularSpectrum(new BetaSpectrum(fssfile), resolution, 18395d, 18580d);
sp.setCaching(false);
//RangedNamedSetSpectrum beta = new BetaSpectrum(fssfile);
//ModularSpectrum sp = new ModularSpectrum(beta, 2.28e-4, 18395d, 18580d);

//        ModularTritiumSpectrum beta = new ModularTritiumSpectrum(2.28e-4, 18395d, 18580d, "d:\\PlayGround\\FS.txt");
NBkgSpectrum spectrum = new NBkgSpectrum(sp);
XYModel model = new XYModel("tritium", spectrum, new SpectrumAdapter());

ParamSet allPars = new ParamSet();

allPars.setParValue("N", 602533.94);
//значение 6е-6 соответствует полной интенстивности 6е7 распадов в секунду
//Проблема была в переполнении счетчика событий в генераторе. Заменил на long. Возможно стоит поставить туда число с плавающей точкой
allPars.setParError("N", 1000);
allPars.setParDomain("N", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("bkg", 0.012497889);
allPars.setParError("bkg", 1e-4);
allPars.setParValue("E0", 18575.986);
allPars.setParError("E0", 0.05);
allPars.setParValue("mnu2", 0d);
allPars.setParError("mnu2", 1d);
allPars.setParValue("msterile2", 50 * 50);
allPars.setParValue("U2", 0);
allPars.setParError("U2", 1e-2);
allPars.setParDomain("U2", -1d, 1d);
allPars.setParValue("X", 0.47);
allPars.setParError("X", 0.014);
allPars.setParDomain("X", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("trap", 1d);
allPars.setParError("trap", 0.2d);
allPars.setParDomain("trap", 0d, Double.POSITIVE_INFINITY);

ListTable data = readData("c:\\Users\\Darksnake\\Dropbox\\PlayGround\\RUN23.DAT", 18400d);

FitState state = new FitState(data, model, allPars);

FitState res = fm.runDefaultStage(state, "E0", "N", "bkg");

res = fm.runDefaultStage(res, "E0", "N", "bkg", "mnu2");

res.print(out);

//spectrum.counter.print(onComplete);
//
////        fm.setPriorProb(new Gaussian("X", 0.47, 0.47*0.03));
////        fm.setPriorProb(new MultivariateGaussianPrior(allPars.getSubSet("X","trap")));
//res = fm.runStage(res, "MINUIT", "run", "E0", "N", "bkg", "mnu2");
////
//res.print(onComplete);

//sp.setCaching(true);
//sp.setSuppressWarnings(true);
//
//BayesianConfidenceLimit bm = new BayesianConfidenceLimit();
//bm.printMarginalLikelihood(onComplete, "U2", res, ["E0", "N", "bkg", "U2", "X"], 10000);

//        PrintNamed.printLike2D(Out.onComplete, "like", res, "N", "E0", 30, 60, 2);
