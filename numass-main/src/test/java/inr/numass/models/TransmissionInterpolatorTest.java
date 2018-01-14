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

import hep.dataforge.context.Global;
import hep.dataforge.plots.XYFunctionPlot;
import hep.dataforge.plots.data.DataPlot;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import inr.numass.NumassPluginKt;

/**
 *
 * @author darksnake
 */
public class TransmissionInterpolatorTest {

    public static void main(String[] args) {
        JFreeChartFrame frame = NumassPluginKt.displayJFreeChart("TransmissionInterpolatorTest");
//JFreeChartFrame.drawFrame("TransmissionInterpolatorTest", null);
        TransmissionInterpolator interpolator = TransmissionInterpolator.fromFile(Global.instance(),
                "d:\\sterile-new\\loss2014-11\\.dataforge\\merge\\empty_sum.onComplete", "Uset", "CR", 15, 0.8, 19002d);
        frame.add(DataPlot.plot("data", interpolator.getX(), interpolator.getY()));
        frame.add(XYFunctionPlot.Companion.plot("interpolated", interpolator.getXmin(), interpolator.getXmax(), 2000, interpolator::value));

//        PrintFunction.printFuntionSimple(new PrintWriter(System.onComplete), interpolator, interpolator.getXmin(), interpolator.getXmax(), 500);
    }

}
