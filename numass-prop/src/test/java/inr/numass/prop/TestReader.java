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
package inr.numass.prop;

import hep.dataforge.context.GlobalContext;
import hep.dataforge.data.FileData;
import hep.dataforge.data.XYDataAdapter;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.PlotFrame;
import hep.dataforge.plots.data.PlottableData;
import hep.dataforge.plots.fx.FXPlotUtils;
import inr.numass.prop.ar.JNAEpisode;
import inr.numass.prop.ar.JNASpectrum;
import inr.numass.prop.ar.ReadJNADataAction;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 *
 * @author darksnake
 */
public class TestReader {

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
//        new MINUITModule().load();

        FileData file = new FileData(new File("c:\\Users\\Darksnake\\Dropbox\\jna_data\\ar37e2.dat"));
        file.setMeta(new MetaBuilder("meta")
                .putValue("timeFile", "c:\\Users\\Darksnake\\Dropbox\\jna_data\\tar37e2.dat")
                .putValue("temperatureFile", "c:\\Users\\Darksnake\\Dropbox\\jna_data\\e2temp.txt")
        );
        JNAEpisode spectra = new ReadJNADataAction(GlobalContext.instance(), null).runOne(file);

        JNASpectrum sp = spectra.get(4);

        System.out.println();
        for (Map.Entry<Double, Long> entry : sp.asMap().entrySet()) {
            System.out.printf("%g\t%d%n", entry.getKey(), entry.getValue());
        }

        PlotFrame frame = FXPlotUtils.displayJFreeChart("JNA test", null);

        frame.add(PlottableData.plot(sp.asDataSet(), new XYDataAdapter("chanel", "count")));

        Meta temps = sp.meta().getNode("temperature");

        System.out.printf("%n%nThe average temperatures are:%n T2 = %f;%n T4 = %f;%n T5 = %f;%n T6 = %f;%n",
                temps.getDouble("T2"),
                temps.getDouble("T4"),
                temps.getDouble("T5"),
                temps.getDouble("T6"));

//        double lowerChanel = 600;
//        double upperChanel = 1100;
//        DataSet data = sp.asDataSet().filter("chanel", lowerChanel, upperChanel);
//        ParamSet params = new ParamSet()
//                .setPar("amp", 2e5, 1e3)
//                .setPar("pos", 800d, 1d)
//                .setPar("sigma", 100d, 1d)
//                .setPar("dsigma", 0d, 1e-1);
//
//        Model model = new XYModel("split-normal", new SplitNormalSpectrum(), new PoissonAdapter("chanel", "count"));
//        FitState state = new FitState(data, model, params);
//        state = new FitManager(GlobalContext.instance()).runDefaultTask(state);
//        state.print(new PrintWriter(System.out));
    }

}
