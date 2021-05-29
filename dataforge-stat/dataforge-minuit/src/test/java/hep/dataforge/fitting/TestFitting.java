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
package hep.dataforge.fitting;

import hep.dataforge.stat.fit.FitResult;
import hep.dataforge.stat.fit.MINUITPlugin;
import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.tables.Table;


/**
 *
 * @author Darksnake
 */
public class TestFitting {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new MINUITPlugin().startGlobal();
        
        int runs = 1000;
        int num = 4;
        
        double[] res = new double[runs];
        double[] errs = new double[runs];
        
        GaussianSpectrum sp = new GaussianSpectrum();
        
        ParamSet allPars = new ParamSet();
        
        allPars.setParValue("pos", 0);
        allPars.setParError("pos", 1e-2);
        allPars.setParValue("w", 1);
        allPars.setParError("w", 1e-2);
        allPars.setParValue("amp", 100);
        allPars.setParError("amp", 1);
        
        FitResult r = null;
        for (int i = 0; i < runs; i++) {
            Table data = sp.sample(0, 1, 100, -3, 3, num);
            r = GaussianSpectrum.fit(data, allPars, "MINUIT");
            res[i] = r.getParameters().getDouble("pos");
            errs[i] = r.getParameters().getError("pos");

//            r.printCovariance(onComplete());
//            GaussianSpectrum.printInvHessian(data, allPars);
        }

//        r.print(Out.onComplete);
        System.out.println();
        double meanerr = 0;
        double meanval = 0;
        double meanval2 = 0;
        
        for (int i = 0; i < errs.length; i++) {
            System.out.printf("%g\t%g%n", res[i], errs[i]);
            meanval += res[i];
            meanval2 += res[i] * res[i];
            meanerr += errs[i];
        }
        
        meanval /= runs;
        meanval2 /= runs;
        meanerr /= runs;

        System.out.printf("The mean value is %g%n", meanval);
        System.out.printf("The mean error is %g%n", meanerr);
        System.out.printf("The sigma is %g%n", Math.sqrt(meanval2 - meanval * meanval));
        
    }
    
}
