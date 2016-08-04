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
package inr.numass.data;

import hep.dataforge.stat.parametric.ParametricFunction;
import static hep.dataforge.maths.MatrixOperations.inverse;
import hep.dataforge.maths.NamedMatrix;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.values.NamedValueSet;
import inr.numass.utils.TritiumUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 *
 * @author Darksnake
 */
public class SpectrumInformation {

    private final ParametricFunction source;

    public SpectrumInformation(ParametricFunction source) {
        this.source = source;
    }

    public NamedMatrix getExpetedCovariance(NamedValueSet set, ListTable data, String... parNames) {
        String[] names = parNames;
        if (names.length == 0) {
            names = source.namesAsArray();
        }
        NamedMatrix info = this.getInformationMatrix(set, data, names);
        RealMatrix cov = inverse(info.getMatrix());
        return new NamedMatrix(cov, names);
    }

    /**
     * Информационная матрица Фишера в предположении, что ошибки пуассоновские
     *
     * @param set
     * @param data
     * @param parNames
     * @return
     */
    public NamedMatrix getInformationMatrix(NamedValueSet set, ListTable data, String... parNames) {
        SpectrumDataAdapter reader = TritiumUtils.adapter();

        String[] names = parNames;
        if (names.length == 0) {
            names = source.namesAsArray();
        }
        assert source.names().contains(set.namesAsArray());
        assert source.names().contains(names);
        RealMatrix res = new Array2DRowRealMatrix(names.length, names.length);

        for (DataPoint dp : data) {
            /*PENDING Тут имеется глобальная неоптимальность связанная с тем,
            * что при каждом вызове вычисляются две производные
            * Нужно вычислять сразу всю матрицу для каждой точки, тогда количество
            * вызовов производных будет строго равно 1.
             */
            res = res.add(getPointInfoMatrix(set, reader.getX(dp).doubleValue(), reader.getTime(dp), names).getMatrix());
        }

        return new NamedMatrix(res, names);
    }

    // формула правильная!
    public double getPoinSignificance(NamedValueSet set, String name1, String name2, double x) {
        return source.derivValue(name1, x, set) * source.derivValue(name2, x, set) / source.value(x, set);
    }

    public NamedMatrix getPointInfoMatrix(NamedValueSet set, double x, double t, String... parNames) {
        assert source.names().contains(set.namesAsArray());

        String[] names = parNames;
        if (names.length == 0) {
            names = set.namesAsArray();
        }

        assert source.names().contains(names);

        RealMatrix res = new Array2DRowRealMatrix(names.length, names.length);

        for (int i = 0; i < names.length; i++) {
            for (int j = i; j < names.length; j++) {
                double value = getPoinSignificance(set, names[i], names[j], x) * t;
                res.setEntry(i, j, value);
                if (i != j) {
                    res.setEntry(j, i, value);
                }
            }

        }
        return new NamedMatrix(res, names);

    }

    /**
     * Зависимость информации Фишера (отнесенной к времени набора) от точки
     * спектра в предположении, что ошибки пуассоновские
     *
     * @param set
     * @param name1
     * @param name2
     * @return
     */
    public UnivariateFunction getSignificanceFunction(final NamedValueSet set, final String name1, final String name2) {
        return (double d) -> getPoinSignificance(set, name1, name2, d);
    }
}
