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
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.stat.fit.ParamSet
import inr.numass.data.SpectrumInformation
import inr.numass.models.BetaSpectrum
import inr.numass.models.ModularSpectrum
import inr.numass.models.NBkgSpectrum
import inr.numass.models.ResolutionFunction
import org.apache.commons.math3.analysis.UnivariateFunction

import static java.util.Locale.setDefault

setDefault(Locale.US);
Global global = Global.instance();
//        global.loadModule(new MINUIT());

//        FitManager fm = new FitManager("data 2013");
UnivariateFunction reolutionTail = {x -> 
    if (x > 1500) {
        return 0.98;
    } else //Intercept = 1.00051, Slope = -1.3552E-5
    {
        return 1.00051 - 1.3552E-5 * x;
    }
};

ModularSpectrum beta = new ModularSpectrum(new BetaSpectrum(),
    new ResolutionFunction(8.3e-5, reolutionTail), 14490d, 19001d);
beta.setCaching(false);
NBkgSpectrum spectrum = new NBkgSpectrum(beta);

//        XYModel model = new XYModel("tritium", spectrum);
ParamSet allPars = new ParamSet();

allPars.setParValue("N", 3090.1458);
//значение 6е-6 соответствует полной интенстивности 6е7 распадов в секунду
//Проблема была в переполнении счетчика событий в генераторе. Заменил на long. Возможно стоит поставить туда число с плавающей точкой
allPars.setParError("N", 6);
allPars.setParDomain("N", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("bkg", 2.2110028);
allPars.setParError("bkg", 0.03);
allPars.setParValue("E0", 18580.742);
allPars.setParError("E0", 2);
allPars.setParValue("mnu2", 0d);
allPars.setParError("mnu2", 1d);
allPars.setParValue("msterile2", 1000 * 1000);
allPars.setParValue("U2", 0);
allPars.setParError("U2", 1e-4);
allPars.setParDomain("U2", -1d, 1d);
allPars.setParValue("X", 1.0);
allPars.setParError("X", 0.01);
allPars.setParDomain("X", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("trap", 1.0d);
allPars.setParError("trap", 0.01d);
allPars.setParDomain("trap", 0d, Double.POSITIVE_INFINITY);

SpectrumInformation sign = new SpectrumInformation(spectrum);

//        double Elow = 14000d;
//        double Eup = 18600d;
//        int numpoints = (int) ((Eup - Elow) / 50);
//        double time = 1e6 / numpoints;
//        DataSet config = getUniformSpectrumConfiguration(Elow, Eup, time, numpoints);
//        NamedMatrix infoMatrix = sign.getInformationMatrix(allPars, config,"U2","E0","N");
//
//        PrintNamed.printNamedMatrix(Out.out, infoMatrix);
//        NamedMatrix cov = sign.getExpetedCovariance(allPars, config,"U2","E0","N");
//
//        PrintWriter onComplete = Global.onComplete();
//
//        printNamedMatrix(out, cov);
//
//        cov = sign.getExpetedCovariance(allPars, config,"U2","E0","N","X");
//
//        printNamedMatrix(out, cov);
//PlotManager pm = new PlotManager();

Map<String, UnivariateFunction> functions = new HashMap<>();

functions.put("U2", sign.getSignificanceFunction(allPars, "U2", "U2"));
//        functions.put("UX", sign.getSignificanceFunction(allPars, "U2", "X"));
functions.put("X", sign.getSignificanceFunction(allPars, "X", "X"));
functions.put("trap", sign.getSignificanceFunction(allPars, "trap", "trap"));
functions.put("E0", sign.getSignificanceFunction(allPars, "E0", "E0"));

MetaBuilder builder = new MetaBuilder("significance");
builder.putValue("from", 14000d);
builder.putValue("to", 18500d);

pm.plotFunction(builder.build(), functions);

//        printFuntionSimple(out(), func, 14000d, 18600d, 200);

