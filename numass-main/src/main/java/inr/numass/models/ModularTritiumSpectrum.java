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

import hep.dataforge.functions.AbstractParametricFunction;
import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.maths.NamedDoubleSet;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.slf4j.LoggerFactory;

/**
 * Modular tritium spectrum with separate calculation for different transmission
 * components
 *
 * @author Darksnake
 */
public class ModularTritiumSpectrum extends AbstractParametricFunction {

    private static final String[] list = {"U2", "E0", "mnu2", "msterile2", "X", "trap"};
    ParametricFunction bareBeta;
    boolean caching;
    private LossCalculator calculator;
    double elow;
    double endpoint;
    File fssfile;
    BivariateFunction resolution;
    List<NamedSpectrumCaching> scatterList;
    NamedSpectrumCaching trapping;

//    NamedSpectrumCaching elastic;
//    NamedSpectrumCaching inelastic;
//    NamedSpectrumCaching inelastic2;
    /**
     *
     * @param resolution
     * @param elow - нижняя граница кэширования. Должна быть с небольшим запасом
     * по отношению к данным
     * @param endpoint - верхняя граница кэширования, может быть без запаса.
     * @param fssFile
     */
    public ModularTritiumSpectrum(BivariateFunction resolution, double elow, double endpoint, File fssFile) {
        super(list);
        assert (endpoint > elow);
        this.elow = elow;
        this.endpoint = endpoint;
        this.fssfile = fssFile;
        this.resolution = resolution;
        this.calculator = LossCalculator.instance();
        setupCache();
    }

    /**
     *
     * @param resA - относительная ширина разрешения
     * @param elow - нижняя граница кэширования. Должна быть с небольшим запасом
     * по отношению к данным
     * @param endpoint - верхняя граница кэширования, может быть без запаса.
     * @param fssFile
     */
    public ModularTritiumSpectrum(double resA, double elow, double endpoint, File fssFile) {
        this(new ResolutionFunction(resA), elow, endpoint, fssFile);
    }

    @Override
    public double derivValue(String parName, double U, NamedDoubleSet set) {
        if (U >= endpoint) {
            return 0;
        }
        double X = this.getX(set);
        switch (parName) {
            case "U2":
            case "E0":
            case "mnu2":
            case "msterile2":
                List<Double> probs = calculator.getLossProbabilities(X);
                updateScatterCache(probs.size() - 1);
                double sum = 0;

                for (int i = 0; i < probs.size(); i++) {
                    sum += probs.get(i) * scatterList.get(i).derivValue(parName, U, set);
                }

                return sum + this.getTrap(set) * this.trapping.derivValue(parName, U, set);
            case "X":
                List<Double> probDerivs = calculator.getLossProbDerivs(X);
                updateScatterCache(probDerivs.size() - 1);
                double derivSum = 0;

                for (int i = 0; i < probDerivs.size(); i++) {
                    derivSum += probDerivs.get(i) * scatterList.get(i).value(U, set);
                }

                return derivSum;

//                return (X / 3 - 0.5) * this.elastic.value(x, set)
//                        + (0.5 - 2 * X / 3) * this.inelastic.value(x, set)
//                        + (X / 3) * this.inelastic2.value(x, set);
            case "trap":
                return this.trapping.value(U, set);
            default:
                return 0;
        }
    }

    private double getTrap(NamedDoubleSet set) {
        return set.getValue("trap");
    }

    private double getX(NamedDoubleSet set) {
        return set.getValue("X");
    }

    @Override
    public boolean providesDeriv(String name) {
        return true;
    }

    public void setCaching(boolean caching) {
        this.caching = caching;
        this.trapping.setCachingEnabled(caching);
        for (NamedSpectrumCaching sp : this.scatterList) {
            sp.setCachingEnabled(caching);
        }
    }

    public void setSuppressWarnings(boolean suppress) {
        this.trapping.setSuppressWarnings(suppress);
        for (NamedSpectrumCaching sp : this.scatterList) {
            sp.setSuppressWarnings(suppress);
            
        }
    }

    /**
     * Отдельный метод нужен на случай, если бета-спектр(FSS) или разрешение
     * будут меняться в процессе
     */
    private void setupCache() {
        if (fssfile == null) {
            bareBeta = new BetaSpectrum();
        } else {
            bareBeta = new BetaSpectrum(fssfile);
        }
        
        //обновляем кэши для трэппинга и упругого прохождения
        BivariateFunction trapFunc = LossCalculator.getTrapFunction();
        BivariateFunction trapRes = new LossResConvolution(trapFunc, resolution);
        
        ParametricFunction elasticSpectrum = new TransmissionConvolution(bareBeta, resolution, endpoint);
        ParametricFunction trapSpectrum = new TransmissionConvolution(bareBeta, trapRes, endpoint);
        scatterList = new ArrayList<>();
        //добавляем нулевой порядок - упругое рассеяние
        scatterList.add(new TritiumSpectrumCaching(elasticSpectrum, elow, endpoint));
        this.trapping = new TritiumSpectrumCaching(trapSpectrum, elow, endpoint);
        /**
         * обнуляем кэш рассеяния
         */
        
    }

    /**
     * Обновляем кэш рассеяния если требуемый порядок выше, чем тот, что есть
     *
     * @param order
     */
    private void updateScatterCache(int order) {
        if (order >= scatterList.size()) {
           LoggerFactory.getLogger(getClass())
                    .debug("Updating scatter cache up to order of '{}'", order);
            // здесь можно сэкономить вызовы, начиная с scatterList.size(), но надо это?
            for (int i = 1; i < order + 1; i++) {
                BivariateFunction loss = calculator.getLossFunction(i);
                BivariateFunction lossRes = new LossResConvolution(loss, resolution);
                ParametricFunction inelasticSpectrum = new TransmissionConvolution(bareBeta, lossRes, endpoint);
                TritiumSpectrumCaching spCatch = new TritiumSpectrumCaching(inelasticSpectrum, elow, endpoint);
                spCatch.setCachingEnabled(caching);
                //TODO сделать пороверку
                scatterList.add(i, spCatch);
            }
        }
    }

    @Override
    public double value(double x, NamedDoubleSet set) {
        if (x >= endpoint) {
            return 0;
        }
        double X = this.getX(set);

        List<Double> probs = calculator.getLossProbabilities(X);
        updateScatterCache(probs.size() - 1);
        double res = 0;

        for (int i = 0; i < probs.size(); i++) {
            res += probs.get(i) * scatterList.get(i).value(x, set);
        }

        return res + this.getTrap(set) * this.trapping.value(x, set);
    }
}
