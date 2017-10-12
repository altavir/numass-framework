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
import hep.dataforge.data.DataSet
import hep.dataforge.stat.fit.FitManager
import hep.dataforge.stat.fit.FitState
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.likelihood.BayesianConfidenceLimit
import hep.dataforge.stat.models.XYModel
import hep.dataforge.tables.ListTable
import inr.numass.data.SpectrumGenerator
import inr.numass.models.BetaSpectrum
import inr.numass.models.ModularSpectrum
import inr.numass.models.NBkgSpectrum

import static hep.dataforge.maths.RandomUtils.setSeed
import static inr.numass.utils.DataModelUtils.getUniformSpectrumConfiguration

PrintWriter out = Global.out();
FitManager fm = new FitManager();

setSeed(543982);

//        TritiumSpectrum beta = new TritiumSpectrum(2e-4, 13995d, 18580d);
File fssfile = new File("c:\\Users\\Darksnake\\Dropbox\\PlayGround\\FS.txt");
ModularSpectrum beta = new ModularSpectrum(new BetaSpectrum(),8.3e-5, 14400d, 19010d);
beta.setCaching(false);
NBkgSpectrum spectrum = new NBkgSpectrum(beta);
XYModel model = new XYModel("tritium", spectrum);

ParamSet allPars = new ParamSet();

allPars.setParValue("N", 6e5);
//значение 6е-6 соответствует полной интенстивности 6е7 распадов в секунду
//Проблема была в переполнении счетчика событий в генераторе. Заменил на long. Возможно стоит поставить туда число с плавающей точкой
allPars.setParError("N", 25);
allPars.setParDomain("N", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("bkg", 5);
allPars.setParError("bkg", 1e-3);
allPars.setParValue("E0", 18575d);
allPars.setParError("E0", 0.1);
allPars.setParValue("mnu2", 0d);
allPars.setParError("mnu2", 1d);
allPars.setParValue("msterile2", 1000 * 1000);
allPars.setParValue("U2", 0);
allPars.setParError("U2", 1e-4);
allPars.setParDomain("U2", 0d, 1d);
allPars.setParValue("X", 0.0);
allPars.setParDomain("X", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("trap", 1d);
allPars.setParError("trap", 0.01d);
allPars.setParDomain("trap", 0d, Double.POSITIVE_INFINITY);

//        PlotPlugin pm = new PlotPlugin();
//        String plotTitle = "Tritium spectrum";
//        pm.plotFunction(ParametricUtils.getSpectrumFunction(spectrum, allPars), 14000, 18600, 500,plotTitle, null);
//        PrintNamed.printSpectrum(Out.onComplete, beta.trapping, allPars, 14000d, 18600d, 500);
//        double e = 18570d;
//        trans.alpha = 1e-4;
//        trans.plotTransmission(System.onComplete, allPars, e, e-1000d, e+100d, 200);
SpectrumGenerator generator = new SpectrumGenerator(model, allPars);

//        ColumnedDataFile file = new ColumnedDataFile("d:\\PlayGround\\RUN36.cfg");
//        ListTable config = file.getPoints("time","X");
double Elow = 14000d;
double Eup = 18600d;
int numpoints = (int) ((Eup - Elow) / 50);
double time = 1e6 / numpoints; // 3600 / numpoints;
DataSet config = getUniformSpectrumConfiguration(Elow, Eup, time, numpoints);
//        config.addAll(DataModelUtils.getUniformSpectrumConfiguration(Eup, Elow, time, numpoints));// в обратную сторону

ListTable data = generator.generateData(config);
//        plotTitle = "Generated tritium spectrum data";
//        pm.plotXYScatter(data, "X", "Y",plotTitle, null);
//        bareBeta.setFSS("D:\\PlayGround\\FSS.dat");
//        data = tritiumUtils.applyDrift(data, 2.8e-6);

FitState state = fm.buildState(data, model, allPars);

//       fm.checkDerivs();
//        res.print(Out.onComplete);
//        fm.checkFitDerivatives();
FitState res = fm.runDefaultStage(state, "U2", "N", "trap");

res.print(out);

//        res = fm.runFrom(res);
//        res = fm.generateErrorsFrom(res);
beta.setCaching(true);
beta.setSuppressWarnings(true);

BayesianConfidenceLimit bm = new BayesianConfidenceLimit();
//        bm.setPriorProb(new OneSidedUniformPrior("trap", 0, true));
//        bm.setPriorProb(new Gaussian("trap", 1d, 0.002));
//        bm.printMarginalLikelihood(Out.onComplete,"U2", res);

FitState conf = bm.getConfidenceInterval("U2", res, ["U2", "N", "trap"]);
//        plotTitle = String.format("Marginal likelihood for parameter \'%s\'", "U2");
//        pm.plotFunction(bm.getMarginalLikelihood("U2", res), 0, 2e-3, 40,plotTitle, null);

conf.print(out);
//        PrintNamed.printLogProbRandom(Out.onComplete, res, 5000,0.5d, "E0","N");

spectrum.counter.print(out);

