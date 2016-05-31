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

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.functions.AbstractParametricFunction;
import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.values.NamedValueSet;
import hep.dataforge.values.ValueProvider;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Experimental differential loss spectrum
 *
 * @author darksnake
 */
public class ExperimentalVariableLossSpectrum extends VariableLossSpectrum {

    public static ExperimentalVariableLossSpectrum withGun(double eGun, double resA, double eMax, double smootherW) {
        return new ExperimentalVariableLossSpectrum(new GunSpectrum(), eMax,smootherW);
    }

    public static ExperimentalVariableLossSpectrum withData(final UnivariateFunction transmission, double eMax, double smootherW) {
        return new ExperimentalVariableLossSpectrum(new AbstractParametricFunction(new String[0]) {

            @Override
            public double derivValue(String parName, double x, NamedValueSet set) {
                throw new NotDefinedException();
            }

            @Override
            public boolean providesDeriv(String name) {
                return false;
            }

            @Override
            public double value(double x, NamedValueSet set) {
                return transmission.value(x);
            }
        }, eMax,smootherW);
    }

    Loss loss;

//    private double smootherW;
//    public ExperimentalVariableLossSpectrum(UnivariateFunction transmission, double eMax, double smootherW) throws NamingException {
//        super(transmission, eMax);
//        loss = new Loss(smootherW);
//    }
    public ExperimentalVariableLossSpectrum(ParametricFunction transmission, double eMax, double smootherW) {
        super(transmission, eMax);
        loss = new Loss(smootherW);
    }

//    public ExperimentalVariableLossSpectrum(double eGun, double resA, double eMax, double smootherW) throws NamingException {
//        super(eGun, resA, eMax);
//        loss = new Loss(smootherW);
//    }
    @Override
    public UnivariateFunction singleScatterFunction(
            double exPos,
            double ionPos,
            double exW,
            double ionW,
            double exIonRatio) {

        return (double eps) -> {
            if (eps <= 0) {
                return 0;
            }

            return (loss.excitation(exPos, exW).value(eps) * exIonRatio + loss.ionization(ionPos, ionW).value(eps)) / (1d + exIonRatio);
        };
    }

    public static class Loss {

        private BivariateFunction smoother;

        private double smootherNorm;

        public Loss(double smootherW) {
            if (smootherW == 0) {
                smoother = (e1, e2) -> 0;
                smootherNorm = 0;
            }

            smoother = (e1, e2) -> {
                double delta = e1 - e2;
                if (delta < 0) {
                    return 0;
                } else {
                    return Math.exp(-delta * delta / 2 / smootherW / smootherW);
                }
            };

            smootherNorm = Math.sqrt(2 * Math.PI) * smootherW / 2;
        }

        public UnivariateFunction total(
                final double exPos,
                final double ionPos,
                final double exW,
                final double ionW,
                final double exIonRatio) {
            return (eps) -> (excitation(exPos, exW).value(eps) * exIonRatio + ionization(ionPos, ionW).value(eps)) / (1d + exIonRatio);
        }

        public UnivariateFunction total(ValueProvider set) {
            final double exPos = set.getDouble("exPos");
            final double ionPos = set.getDouble("ionPos");
            final double exW = set.getDouble("exW");
            final double ionW = set.getDouble("ionW");
            final double exIonRatio = set.getDouble("exIonRatio");
            return total(exPos, ionPos, exW, ionW, exIonRatio);
        }

        /**
         * Excitation spectrum
         *
         * @param exPos
         * @param exW
         * @return
         */
        public UnivariateFunction excitation(double exPos, double exW) {
            return (double eps) -> {
                double z = eps - exPos;
                double res;

                double norm = smootherNorm + Math.sqrt(Math.PI / 2) * exW / 2;

                if (z < 0) {
                    res = Math.exp(-2 * z * z / exW / exW);
                } else {
                    res = smoother.value(z, 0);
                }

                return res / norm;
            };
        }

        /**
         * Ionization spectrum
         *
         * @param ionPos
         * @param ionW
         * @return
         */
        public UnivariateFunction ionization(double ionPos, double ionW) {
            return (double eps) -> {
                double res;
                double norm = smootherNorm + ionW * Math.PI / 4d;

                if (eps - ionPos > 0) {
                    res = 1 / (1 + 4 * (eps - ionPos) * (eps - ionPos) / ionW / ionW);
                } else {
                    res = smoother.value(0, eps - ionPos);
                }

                return res / norm;
            };
        }
    }

}
