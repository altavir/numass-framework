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
package inr.numass.run;

import hep.dataforge.context.GlobalContext;
import hep.dataforge.datafitter.MINUITPlugin;
import hep.dataforge.datafitter.ParamSet;
import hep.dataforge.exceptions.NamingException;
import inr.numass.models.BetaSpectrum;
import inr.numass.models.ModularSpectrum;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import static java.util.Locale.setDefault;

/**
 *
 * @author Darksnake
 */
public class NumassSpectrumTest {

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws NamingException, FileNotFoundException {
        setDefault(Locale.US);
        GlobalContext global = GlobalContext.instance();
        global.loadPlugin(new MINUITPlugin());

        ParamSet allPars = new ParamSet();

        allPars.setParValue("N", 3000);
        //значение 6е-6 соответствует полной интенстивности 6е7 распадов в секунду
        //Проблема была в переполнении счетчика событий в генераторе. Заменил на long. Возможно стоит поставить туда число с плавающей точкой
        allPars.setParError("N", 6);
        allPars.setParDomain("N", 0d, Double.POSITIVE_INFINITY);
        allPars.setParValue("bkg", 3);
        allPars.setParError("bkg", 0.03);
        allPars.setParValue("E0", 18500.0);
        allPars.setParError("E0", 2);
        allPars.setParValue("mnu2", 0d);
        allPars.setParError("mnu2", 1d);
        allPars.setParValue("msterile2", 2000 * 2000);
        allPars.setParValue("U2", 0);
        allPars.setParError("U2", 1e-4);
        allPars.setParDomain("U2", -1d, 1d);
        allPars.setParValue("X", 0);
        allPars.setParError("X", 0.01);
        allPars.setParDomain("X", 0d, Double.POSITIVE_INFINITY);
        allPars.setParValue("trap", 1);
        allPars.setParError("trap", 0.01d);
        allPars.setParDomain("trap", 0d, Double.POSITIVE_INFINITY);

        ModularSpectrum betaNew = new ModularSpectrum(new BetaSpectrum(new File("d:\\PlayGround\\FS.txt")), 1e-4, 14390d, 19001d);
        betaNew.setCaching(false);

        System.out.println(betaNew.value(17000d, allPars));

    }
}
