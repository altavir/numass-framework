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
import hep.dataforge.io.FittingIOUtils
import hep.dataforge.io.PrintFunction
import hep.dataforge.maths.NamedDoubleArray
import hep.dataforge.maths.NamedDoubleSet
import hep.dataforge.maths.NamedMatrix
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.PlottableFunction
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import inr.numass.actions.ShowLossSpectrumAction;
import inr.numass.models.ExperimentalVariableLossSpectrum
import inr.numass.models.LossCalculator
import org.apache.commons.math3.analysis.UnivariateFunction


NamedDoubleSet transformOldMeans(NamedDoubleSet old){
    String[] names = ["exPos", "ionPos", "exW", "ionW", "exIonRatio"];
    double[] values = new double[5];
    values[0] = old.getValue("loss_pos1");
    values[1] = old.getValue("loss_pos2");
    values[2] = old.getValue("loss_w1");
    values[3] = old.getValue("loss_w2");
    values[4] = old.getValue("loss_A1") / old.getValue("loss_A2");    

    return new NamedDoubleArray(names, values);

}

ParamSet transformOldParams(ParamSet old){
    String[] names = ["exPos", "ionPos", "exW", "ionW", "exIonRatio"];
    ParamSet res = new ParamSet(names);
    
    res.setPar("exPos", old.getValue("loss_pos1"), old.getError("loss_pos1"));
    res.setPar("ionPos", old.getValue("loss_pos2"), old.getError("loss_pos2"));
    res.setPar("exW", old.getValue("loss_w1"), old.getError("loss_w1"));
    res.setPar("ionW", old.getValue("loss_w2"), old.getError("loss_w2"));
    
    double ratioValue = old.getValue("loss_A1") / old.getValue("loss_A2");
    
    double a1RelErr = old.getError("loss_A1") / old.getValue("loss_A1");
    double a2RelErr = old.getError("loss_A2") / old.getValue("loss_A2");
    double ratioErr = Math.sqrt(a1RelErr*a1RelErr + a2RelErr*a2RelErr)*ratioValue;
    res.setPar("exIonRatio", ratioValue, ratioErr);
    
    return res;
}

PrintWriter out = new PrintWriter(System.out);

String name = "log14_h2_new"
//String name = "log18_long"
//String name = "log25_long"

OldFitResultReader reader = new OldFitResultReader();
reader.readFile(new File("C:\\Users\\darksnake\\Dropbox\\Numass\\Analysis\\loss-2014_november\\old_loss\\results\\final\\${name}.txt"));

double threshold = 17d;

ParamSet means = transformOldParams (reader.getParamSet());

double ionRatio = ShowLossSpectrumAction.calcultateIonRatio(means, threshold);
NamedMatrix cov = NamedMatrix.diagonal(means.getParErrors());
double ionRatioError = ShowLossSpectrumAction.calultateIonRatioError(name, means, cov, threshold);

println String.format("The ionization ratio (using threshold %f) is %f%n", threshold, ionRatio);
println String.format("The ionization ratio standard deviation (using threshold %f) is %f%n", threshold, ionRatioError);
println "*** FIT RESULT ***"

FittingIOUtils.printParamSet(out, means);

println means.toString();
  
UnivariateFunction scatterFunction =  LossCalculator.getSingleScatterFunction(means);
PlotFrame frame = JFreeChartFrame.drawFrame("Differential scattering crossection for "+name, null);
frame.add(new PlottableFunction("Cross-section", null, scatterFunction, 0, 100, 1000));

PrintFunction.printFunctionSimple(out, scatterFunction, 0, 100, 500);