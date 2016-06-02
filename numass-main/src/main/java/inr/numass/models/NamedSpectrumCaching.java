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
import static hep.dataforge.functions.FunctionUtils.getSpectrumDerivativeFunction;
import static hep.dataforge.functions.FunctionUtils.getSpectrumFunction;
import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.maths.MathUtils;
import hep.dataforge.maths.NamedVector;
import hep.dataforge.names.AbstractNamedSet;
import hep.dataforge.values.NamedValueSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Darksnake
 */
public class NamedSpectrumCaching extends AbstractParametricFunction {

    double a;
    double b;
    private boolean cachingEnabled = true;
    public int interpolationNodes = 200;
    ParametricFunction source;
    CacheElement spectrumCache = null;
    Map<String, CacheElement> spectrumDerivCache;
//    CacheElement[] spectrumDerivCache;
    private boolean suppressWarnings = false;

    public NamedSpectrumCaching(ParametricFunction spectrum, double a, double b) {
        super(spectrum);
        assert b > a;
        this.a = a;
        this.b = b;
        this.source = spectrum;
        spectrumDerivCache = new HashMap<>(source.getDimension());
//        spectrumDerivCache = new CacheElement[source.getDimension()];
    }

    public NamedSpectrumCaching(ParametricFunction spectrum, double a, double b, int numPoints) {
        this(spectrum, a, b);
        this.interpolationNodes = numPoints;
    }

    @Override
    public double derivValue(String parName, double x, NamedValueSet set) {
        if (!isCachingEnabled()) {
            return source.derivValue(parName, x, set);
        }

        if (!spectrumDerivCache.containsKey(parName)) {
            if (!suppressWarnings) {
                LoggerFactory.getLogger(getClass())
                        .debug("Starting initial caching of spectrum partial derivative for parameter '{}'", parName);
            }
            CacheElement el = new CacheElement(set, parName);
            spectrumDerivCache.put(parName, el);
            return el.value(x);
        } else {
            CacheElement el = spectrumDerivCache.get(parName);
            if (sameSet(set, el.getCachedParameters())) {
                return el.value(x);
            } else {
                try {
                    return transformation(el, set, x);
                } catch (TransformationNotAvailable ex) {
                    if (!suppressWarnings) {
                        LoggerFactory.getLogger(getClass())
                                .debug("Transformation of cache is not available. Updating cache.");
                    }
                    el = new CacheElement(set, parName);
                    spectrumDerivCache.put(parName, el);
                    return el.value(x);
                }
            }
        }
    }

    /**
     * @return the cachingEnabled
     */
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    @Override
    public boolean providesDeriv(String name) {
        return source.providesDeriv(name);
    }

    protected boolean sameSet(NamedValueSet set1, NamedValueSet set2) {
        for (String name : this.names()) {
            if (!Objects.equals(set1.getDouble(name), set2.getDouble(name))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param cachingEnabled the cachingEnabled to set
     */
    public void setCachingEnabled(boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
    }

    /**
     * @param suppressWarnings the suppressWarnings to set
     */
    public void setSuppressWarnings(boolean suppressWarnings) {
        this.suppressWarnings = suppressWarnings;
    }

    /*
     * Подразумевается, что трансформация одна и та же и для спектра, и для производных.
     */
    protected double transformation(CacheElement cache, NamedValueSet newSet, double x) throws TransformationNotAvailable {


        /*
         * В этом варианте кэширование работает тольеко если все параметры в точности совпадают. 
         * Для конкретных преобразований нужно переопределить этот метод.
         * 
         */
        throw new TransformationNotAvailable();
    }

    @Override
    public double value(double x, NamedValueSet set) {
        if (!isCachingEnabled()) {
            return source.value(x, set);
        }

        if (spectrumCache == null) {
            if (!suppressWarnings) {
                LoggerFactory.getLogger(getClass())
                        .debug("Starting initial caching of spectrum.");
            }
            spectrumCache = new CacheElement(set);
            return spectrumCache.value(x);
        }

        if (sameSet(set, spectrumCache.getCachedParameters())) {
            return spectrumCache.value(x);
        } else {
            try {
                return transformation(spectrumCache, set, x);
            } catch (TransformationNotAvailable ex) {
                if (!suppressWarnings) {
                    LoggerFactory.getLogger(getClass())
                            .debug("Transformation of cache is not available. Updating cache.");
                }
                spectrumCache = new CacheElement(set);
                return spectrumCache.value(x);
            }
        }

    }

    protected static class TransformationNotAvailable extends Exception {
    }

    protected class CacheElement extends AbstractNamedSet implements UnivariateFunction {

        private UnivariateFunction cachedSpectrum;
        private final NamedValueSet cachedParameters;
        String parName;

        CacheElement(NamedValueSet parameters, String parName) {
            super(source);
            //на всякий случай обрезаем набор параметров до необходимого
            String[] names = source.namesAsArray();
            this.cachedParameters = new NamedVector(names, MathUtils.getDoubleArray(parameters));
            UnivariateFunction func = getSpectrumDerivativeFunction(parName, source, parameters);
            generate(func);
        }

        CacheElement(NamedValueSet parameters) {
            super(source);
            String[] names = source.namesAsArray();
            this.cachedParameters = new NamedVector(names, MathUtils.getDoubleArray(parameters));
            UnivariateFunction func = getSpectrumFunction(source, parameters);
            generate(func);
        }

        private void generate(UnivariateFunction func) {
            SplineInterpolator interpolator = new SplineInterpolator();
            double[] x = new double[interpolationNodes];
            double[] y = new double[interpolationNodes];
            double step = (b - a) / (interpolationNodes - 1);
            x[0] = a;
            y[0] = func.value(a);
            for (int i = 1; i < y.length; i++) {
                x[i] = x[i - 1] + step;
                y[i] = func.value(x[i]);

            }
            this.cachedSpectrum = interpolator.interpolate(x, y);
        }

        @Override
        public double value(double x) {
            return this.cachedSpectrum.value(x);
        }

        public NamedValueSet getCachedParameters() {
            return this.cachedParameters;
        }
    }
}
