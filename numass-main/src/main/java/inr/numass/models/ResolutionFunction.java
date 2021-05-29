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

import hep.dataforge.maths.Interpolation;
import java.io.InputStream;
import static java.lang.Double.isNaN;
import static java.lang.Math.sqrt;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Функция разрешения в точном и упрощенном виде. Возможность фитировать
 * разрешение пока не предусмотрена.
 *
 * @author Darksnake
 */
public class ResolutionFunction implements BivariateFunction {

    public static BivariateFunction getRealTail() {
        InputStream transmissionPointStream = ResolutionFunction.class.getResourceAsStream("/numass/models/transmission");
        Scanner scanner = new Scanner(transmissionPointStream);

        Map<Number, Number> values = new HashMap<>();

        while (scanner.hasNextDouble()) {
            values.put((18.5 - scanner.nextDouble()) * 1000, scanner.nextDouble());
        }

        UnivariateFunction f = Interpolation.interpolate(values, Interpolation.InterpolationType.LINE, Double.NaN, Double.NaN);

        return (double x, double y) -> f.value(x - y);
    }

    public static BivariateFunction getAngledTail(double dropPerKv) {
        return (double E, double U) -> 1 - (E - U) * dropPerKv / 1000d;
    }

    /**
     * (E, U) -> 1 - (E - U) * (alpha + E * beta) / 1000d
     * @param alpha drop per kV at E = 0
     * @param beta dependence of drop per kV on E (in kV)
     * @return 
     */
    public static BivariateFunction getAngledTail(double alpha, double beta) {
        return (double E, double U) -> 1 - (E - U) * (alpha + E /1000d * beta) / 1000d;
    }

    public static BivariateFunction getConstantTail() {
        return new ConstantTailFunction();
    }

    private final double resA;
    private double resB = Double.NaN;
    private BivariateFunction tailFunction = new ConstantTailFunction();

    /**
     * Если исползуется конструктор с одним параметром, то по-умолчанию
     * используем упрощенную схему.
     *
     * @param resA
     */
    public ResolutionFunction(double resA) {
        this.resA = resA;
    }

    public ResolutionFunction(double resA, BivariateFunction tailFunction) {
        this.resA = resA;
        this.tailFunction = tailFunction;
    }

    ResolutionFunction(double resA, double resB) {
        this.resA = resA;
        this.resB = resB;
    }

    private double getValueFast(double E, double U) {
        double delta = resA * E;
        if (E - U < 0) {
            return 0;
        } else if (E - U > delta) {
            return tailFunction.value(E, U);
        } else {
            return (E - U) / delta;
        }
    }

    public void setTailFunction(BivariateFunction tailFunction) {
        this.tailFunction = tailFunction;
    }

    @Override
    public double value(double E, double U) {
        assert resA > 0;
        if (isNaN(resB)) {
            return this.getValueFast(E, U);
        }
        assert resB > 0;
        double delta = resA * E;
        if (E - U < 0) {
            return 0;
        } else if (E - U > delta) {
            return tailFunction.value(E, U);
        } else {
            return (1 - sqrt(1 - (E - U) / E * resB)) / (1 - sqrt(1 - resA * resB));
        }
    }

    private static class ConstantTailFunction implements BivariateFunction {

        @Override
        public double value(double x, double y) {
            return 1;
        }

    }

}
