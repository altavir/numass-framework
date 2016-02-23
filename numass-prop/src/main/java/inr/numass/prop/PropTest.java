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
import static hep.dataforge.context.GlobalContext.out;
import hep.dataforge.data.DataPoint;
import hep.dataforge.datafitter.FitManager;
import hep.dataforge.datafitter.FitState;
import hep.dataforge.datafitter.ParamSet;
import hep.dataforge.datafitter.models.HistogramGenerator;
import hep.dataforge.datafitter.models.HistogramModel;
import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.maths.MatrixOperations;
import hep.dataforge.maths.RandomUtils;
import inr.numass.models.BetaSpectrum;
import inr.numass.models.NBkgSpectrum;
import java.io.FileNotFoundException;
import hep.dataforge.data.PointSet;

/**
 * Hello world!
 *
 */
public class PropTest {

    public static void main(String[] args) throws FileNotFoundException {

        FitManager fm = new FitManager();

        RandomUtils.setSeed(138);
        GlobalContext.instance().putValue(MatrixOperations.MATRIX_SINGULARITY_THRESHOLD, 1e-80);

        BetaSpectrum bareBeta = new BetaSpectrum();
        PropResolution trans = new PropResolution(new BaseFunction());

        ParametricFunction convolutedBeta = trans.getConvolutedSpectrum(bareBeta);
        NBkgSpectrum spectrum = new NBkgSpectrum(convolutedBeta);

//        XYModel model = new XYModel("tritium", spectrum);
        HistogramModel model = new HistogramModel("tritium-prop", spectrum);

        ParamSet allPars = new ParamSet(model.namesAsArray());

        allPars.setPar("N", 1e8, 1e3, 0d, Double.POSITIVE_INFINITY)
                .setPar("bkg", 1, 1e-4)
                .setPar("E0", 18575d, 5)
                .setPar("mnu2", 0d, 1d)
                .setPar("msterile2", 6.4e7, 1e2)
                .setPar("U2", 0, 1e-3)
                .setPar("w", 5.5, 5e-3)
                .setPar("dw", 1e-2, 1e-3)
                .setPar("base", 0.05, 0.1);

//        NamedSpectrum response = trans.getResponseFunction(2700d);
//        
//        pm.plotFunction(FunctionUtils.getSpectrumFunction(response, allPars), 100, 4000, 400);
        //   pm.plotFunction(FunctionUtils.getSpectrumFunction(spectrum, allPars), 100, 19000, 400);
        //pm.plotFunction(trans.getProduct(bareBeta, allPars, 9000d), 1000d, 19000d, 400);
//        pm.plotFunction(FunctionUtils.fix1stArgument(trans.getBivariateFunction(allPars), 14000d), 1000, 18000, 400);
        HistogramGenerator generator = new HistogramGenerator(null, model, allPars);
        PointSet data = generator.generateUniformHistogram(1000d, 18500d, 350);

        long count = 0;
        for (DataPoint dp : data) {
            count += dp.getValue("count").longValue();
        }

        out().printf("The total number of events is %g%n", (double) count);

//        allPars.setParValue("base", 1e-3);
//        allPars.setParValue("w", 5.470);        
        allPars.setParValue("dw", 2e-2);         
        FitState state = FitManager.buildState(data, model, allPars);

        FitState res = fm.runDefaultTask(state, "U2", "E0", "N");
        res.print(out());
//        log.print(out());
    }
}
