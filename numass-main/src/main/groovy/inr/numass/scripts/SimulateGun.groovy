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
import hep.dataforge.io.FittingIOUtils
import hep.dataforge.stat.fit.FitManager
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.models.XYModel
import inr.numass.data.SpectrumAdapter
import inr.numass.models.GunSpectrum
import inr.numass.models.NBkgSpectrum

import static java.util.Locale.setDefault

setDefault(Locale.US);
Global global = Global.instance();
//        global.loadModule(new MINUITModule());

FitManager fm = new FitManager();

GunSpectrum gsp = new GunSpectrum();
NBkgSpectrum spectrum = new NBkgSpectrum(gsp);

XYModel model = new XYModel("gun", spectrum, new SpectrumAdapter());

ParamSet allPars = new ParamSet()
.setPar("N", 1e3, 1e2)
.setPar("pos", 18500, 0.1)
.setPar("bkg", 50, 1)
.setPar("resA", 5.3e-5, 1e-5)
.setPar("sigma", 0.3, 0.03);

PrintNamed.printSpectrum(new PrintWriter(System.out), spectrum, allPars, 18495, 18505, 100);

allPars.setParValue("sigma", 0.6);

FittingIOUtils.printSpectrum(new PrintWriter(System.out), spectrum, allPars, 18495, 18505, 100);

//        //String fileName = "d:\\PlayGround\\merge\\scans.onComplete";
////        String configName = "d:\\PlayGround\\SCAN.CFG";
////        ListTable config = OldDataReader.readConfig(configName);
//        SpectrumGenerator generator = new SpectrumGenerator(model, allPars, 12316);
//
//        ListTable data = generator.generateData(DataModelUtils.getUniformSpectrumConfiguration(18495, 18505, 20, 20));
//
////        data = data.filter("X", Value.of(15510.0), Value.of(18610.0));
////        allPars.setParValue("X", 0.4);
//        FitState state = FitTaskManager.buildState(data, model, allPars);
//
//        FitState res = fm.runStage(state, "QOW", FitTask.TASK_RUN, "N", "bkg", "pos", "sigma");
//
//        res.print(onComplete());

