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

import hep.dataforge.plots.data.PlotXYFunction
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import org.apache.commons.math3.analysis.UnivariateFunction


def lorenz = {x, x0, gama -> 1/(3.14*gama*(1+(x-x0)*(x-x0)/gama/gama))}

    
def excitationSpectrum = {Map<Double,Double> lines, double gama ->
    UnivariateFunction function = {x->
        double res = 0;
        lines.each{k,v -> res += lorenz(x,k,gama)*v};
        return res;
    }
    return function;
}

def lines =
[
    12.6:0.5,
    12.4:0.3,
    12.2:0.2
]

UnivariateFunction excitation = excitationSpectrum(lines,0.08)

JFreeChartFrame frame = JFreeChartFrame.drawFrame("theoretical loss spectrum", null);

frame.add(PlotXYFunction.plotFunction("excitation", excitation, 0d, 20d, 500));