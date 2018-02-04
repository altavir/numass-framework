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
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.meta.Meta
import hep.dataforge.stat.fit.FitManager
import hep.dataforge.stat.fit.FitStage
import hep.dataforge.stat.fit.FitState
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.models.XYModel
import inr.numass.NumassPlugin
import inr.numass.data.SpectrumAdapter
import inr.numass.data.SpectrumGenerator
import inr.numass.models.NBkgSpectrum
import inr.numass.models.sterile.SterileNeutrinoSpectrum
import inr.numass.utils.DataModelUtils

import static hep.dataforge.context.Global.out
import static java.util.Locale.setDefault

/**
 *
 * @author Darksnake
 */

setDefault(Locale.US);
new NumassPlugin().startGlobal()
FitManager fm = Global.instance().get(FitManager)


SterileNeutrinoSpectrum sp = new SterileNeutrinoSpectrum(Global.instance(), Meta.empty());
//beta.setCaching(false);

NBkgSpectrum spectrum = new NBkgSpectrum(sp);
XYModel model = new XYModel(Meta.empty(), new SpectrumAdapter(), spectrum);

ParamSet allPars = new ParamSet();

allPars.setParValue("N", 2e6 / 100);
//значение 6е-6 соответствует полной интенстивности 6е7 распадов в секунду
//Проблема была в переполнении счетчика событий в генераторе. Заменил на long. Возможно стоит поставить туда число с плавающей точкой
allPars.setParError("N", 6);
allPars.setParDomain("N", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("bkg", 2d);
allPars.setParError("bkg", 0.03);
allPars.setParValue("E0", 18575.0);
allPars.setParError("E0", 2);
allPars.setParValue("mnu2", 0d);
allPars.setParError("mnu2", 1d);
allPars.setParValue("msterile2", 8000 * 8000);
allPars.setParValue("U2", 0);
allPars.setParError("U2", 1e-4);
allPars.setParDomain("U2", -1d, 1d);
allPars.setParValue("X", 0);
allPars.setParError("X", 0.01);
allPars.setParDomain("X", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("trap", 0d);
allPars.setParError("trap", 0.01d);
allPars.setParDomain("trap", 0d, Double.POSITIVE_INFINITY);

//        PrintNamed.printSpectrum(Global.onComplete(), spectrum, allPars, 0.0, 18700.0, 600);
//String fileName = "d:\\PlayGround\\merge\\scans.onComplete";
//        String configName = "d:\\PlayGround\\SCAN.CFG";
//        ListTable config = OldDataReader.readConfig(configName);
SpectrumGenerator generator = new SpectrumGenerator(model, allPars, 12316);

def data = generator.generateData(DataModelUtils.getUniformSpectrumConfiguration(14000, 18500, 604800 / 100 * 100, 100));

//data = TritiumUtils.correctForDeadTime(data, new SpectrumAdapter(), 10e-9);
//        data = data.filter("X", Value.of(15510.0), Value.of(18610.0));
//        allPars.setParValue("X", 0.4);


ColumnedDataWriter.writeTable(System.out, data, "--- DATA ---");
FitState state = new FitState(data, model, allPars);
//new PlotFitResultAction().eval(state);


def res = fm.runStage(state, "QOW", FitStage.TASK_RUN, "N", "bkg", "E0", "U2");



res.print(out());

