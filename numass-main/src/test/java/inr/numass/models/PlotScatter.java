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
package inr.numass.models;

import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.plots.fx.FXPlotUtils;

/**
 *
 * @author darksnake
 */
public class PlotScatter {

    public static void main(String[] args) {
        ParamSet pars = ParamSet.fromString(
                  "'N'	= 2492.87 ± 3.6	(0.00000,Infinity)\n"
                + "'bkg'	= 5.43 ± 0.16\n"
                + "'X'	= 0.51534 ± 0.0016\n"
                + "'shift'	= 0.00842 ± 0.0024\n"
                + "'exPos'	= 12.870 ± 0.054\n"
                + "'ionPos'	= 16.63 ± 0.58\n"
                + "'exW'	= 1.49 ± 0.15\n"
                + "'ionW'	= 11.33 ± 0.43\n"
                + "'exIonRatio'	= 4.83 ± 0.36"
        );
        LossCalculator.plotScatter(FXPlotUtils.displayJFreeChart("Loss function", null),pars);
    }
}
