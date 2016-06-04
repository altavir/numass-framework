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
import hep.dataforge.names.NamedUtils;
import hep.dataforge.values.NamedValueSet;
import hep.dataforge.values.ValueProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.slf4j.LoggerFactory;

/**
 * Modular spectrum for any source spectrum with separate calculation for
 * different transmission components
 *
 * @author Darksnake
 */
public class ModularSpectrum extends AbstractParametricFunction {

    private static final String[] list = {"X", "trap"};
    private LossCalculator calculator;
    List<NamedSpectrumCaching> cacheList;
    NamedSpectrumCaching trappingCache;
    BivariateFunction resolution;
    RangedNamedSetSpectrum sourceSpectrum;
    BivariateFunction trappingFunction;
    boolean caching = true;
    double cacheMin;
    double cacheMax;

    /**
     *
     * @param source
     * @param resolution
     * @param cacheMin - нижняя граница кэширования. Должна быть с небольшим
     * запасом по отношению к данным
     * @param cacheMax - верхняя граница кэширования.
     */
    public ModularSpectrum(RangedNamedSetSpectrum source, BivariateFunction resolution, double cacheMin, double cacheMax) {
        super(NamedUtils.combineNamesWithEquals(list, source.namesAsArray()));
        if (cacheMin >= cacheMax) {
            throw new IllegalArgumentException();
        }
        this.cacheMin = cacheMin;
        this.cacheMax = cacheMax;
        this.resolution = resolution;
        this.calculator = LossCalculator.instance();
        this.sourceSpectrum = source;
        setupCache();
    }

    public ModularSpectrum(RangedNamedSetSpectrum source, BivariateFunction resolution) {
        this(source, resolution, Double.NaN, Double.NaN);
        setCaching(false);
    }

    /**
     *
     * @param source
     * @param resA - относительная ширина разрешения
     * @param cacheMin - нижняя граница кэширования. Должна быть с небольшим
     * запасом по отношению к данным
     * @param cacheMax - верхняя граница кэширования, может быть без запаса.
     */
    public ModularSpectrum(RangedNamedSetSpectrum source, double resA, double cacheMin, double cacheMax) {
        this(source, new ResolutionFunction(resA), cacheMin, cacheMax);
    }

    public ModularSpectrum(RangedNamedSetSpectrum source, double resA) {
        this(source, new ResolutionFunction(resA));
    }

    public void setTrappingFunction(BivariateFunction trappingFunction) {
        this.trappingFunction = trappingFunction;
        LoggerFactory.getLogger(getClass()).info("Recalculating modular spectrum cache");
        setupCache();
    }

    
    
    /**
     * Отдельный метод нужен на случай, если бета-спектр(FSS) или разрешение
     * будут меняться в процессе
     */
    private void setupCache() {

        //обновляем кэши для трэппинга и упругого прохождения
        //Using external trappingCache function if provided
        BivariateFunction trapFunc = trappingFunction != null ? trappingFunction : LossCalculator.getTrapFunction();
        BivariateFunction trapRes = new LossResConvolution(trapFunc, resolution);

        ParametricFunction elasticSpectrum = new TransmissionConvolution(sourceSpectrum, resolution, sourceSpectrum);
        ParametricFunction trapSpectrum = new TransmissionConvolution(sourceSpectrum, trapRes, sourceSpectrum);
        /**
         * обнуляем кэш рассеяния
         */
        cacheList = new ArrayList<>();
        //добавляем нулевой порядок - упругое рассеяние

        TritiumSpectrumCaching elasticCache = new TritiumSpectrumCaching(elasticSpectrum, cacheMin, cacheMax);
        elasticCache.setCachingEnabled(caching);
        cacheList.add(elasticCache);
        this.trappingCache = new TritiumSpectrumCaching(trapSpectrum, cacheMin, cacheMax);
        this.trappingCache.setCachingEnabled(caching);
    }

    /**
     * Обновляем кэш рассеяния если требуемый порядок выше, чем тот, что есть
     *
     * @param order
     */
    private void updateScatterCache(int order) {
        if (order >= cacheList.size()) {
            LoggerFactory.getLogger(getClass())
                    .debug("Updating scatter cache up to order of '{}'", order);
            // здесь можно сэкономить вызовы, начиная с cacheList.size(), но надо это?
            for (int i = 1; i < order + 1; i++) {
                BivariateFunction loss = calculator.getLossFunction(i);
                BivariateFunction lossRes = new LossResConvolution(loss, resolution);
                ParametricFunction inelasticSpectrum = new TransmissionConvolution(sourceSpectrum, lossRes, sourceSpectrum);
                TritiumSpectrumCaching spCatch = new TritiumSpectrumCaching(inelasticSpectrum, cacheMin, cacheMax);
                spCatch.setCachingEnabled(caching);
                spCatch.setSuppressWarnings(true);
                //TODO сделать пороверку 
                cacheList.add(i, spCatch);
            }
        }
    }

    @Override
    public double derivValue(String parName, double U, NamedValueSet set) {
        if (U >= sourceSpectrum.max(set)) {
            return 0;
        }
        double X = this.getX(set);
        switch (parName) {
            case "X":
                List<Double> probDerivs = calculator.getLossProbDerivs(X);
                updateScatterCache(probDerivs.size() - 1);
                double derivSum = 0;

                for (int i = 0; i < probDerivs.size(); i++) {
                    derivSum += probDerivs.get(i) * cacheList.get(i).value(U, set);
                }

                return derivSum;
            case "trap":
                return this.trappingCache.value(U, set);
            default:
                if (sourceSpectrum.names().contains(parName)) {
                    List<Double> probs = calculator.getLossProbabilities(X);
                    updateScatterCache(probs.size() - 1);
                    double sum = 0;

                    for (int i = 0; i < probs.size(); i++) {
                        sum += probs.get(i) * cacheList.get(i).derivValue(parName, U, set);
                    }

                    sum += this.getTrap(set) * this.trappingCache.derivValue(parName, U, set);
                    return sum;
                } else {
                    return 0;
                }
        }
    }

    private double getTrap(ValueProvider set) {
        return set.getDouble("trap");
    }

    private double getX(ValueProvider set) {
        return set.getDouble("X");
    }

    @Override
    public boolean providesDeriv(String name) {
        return sourceSpectrum.providesDeriv(name);
    }

    /**
     * Set the boundaries and recalculate cache
     *
     * @param cacheMin
     * @param cacheMax
     */
    public void setCachingBoundaries(double cacheMin, double cacheMax) {
        this.cacheMin = cacheMin;
        this.cacheMax = cacheMax;
        setupCache();
    }

    public final void setCaching(boolean caching) {
        if (caching && (cacheMin == Double.NaN || cacheMax == Double.NaN)) {
            throw new IllegalStateException("Cahing boundaries are not defined");
        }

        this.caching = caching;
        this.trappingCache.setCachingEnabled(caching);
        for (NamedSpectrumCaching sp : this.cacheList) {
            sp.setCachingEnabled(caching);
        }
    }

    /**
     * Suppress warnings about cache recalculation
     * @param suppress 
     */
    public void setSuppressWarnings(boolean suppress) {
        this.trappingCache.setSuppressWarnings(suppress);
        this.cacheList.stream().forEach((sp) -> {
            sp.setSuppressWarnings(suppress);
        });
    }

    @Override
    public double value(double U, NamedValueSet set) {
        if (U >= sourceSpectrum.max(set)) {
            return 0;
        }
        double X = this.getX(set);

        List<Double> probs = calculator.getLossProbabilities(X);
        updateScatterCache(probs.size() - 1);
        double res = 0;

        for (int i = 0; i < probs.size(); i++) {
            res += probs.get(i) * cacheList.get(i).value(U, set);
        }

        res += this.getTrap(set) * this.trappingCache.value(U, set);
        return res;
    }
}
