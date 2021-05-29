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

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.maths.GridCalculator;
import hep.dataforge.maths.NamedVector;
import hep.dataforge.meta.Meta;
import hep.dataforge.names.AbstractNamedSet;
import hep.dataforge.stat.fit.FitManager;
import hep.dataforge.stat.fit.FitResult;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.stat.models.XYModel;
import hep.dataforge.stat.parametric.ParametricFunction;
import hep.dataforge.tables.Adapters;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Values;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 *
 * @author Darksnake
 */
public class GaussianSpectrum extends AbstractNamedSet implements ParametricFunction {

    private static final String[] list = {"w", "pos", "amp"};
    private final RandomGenerator rnd;

    public GaussianSpectrum() {
        super(list);
        rnd = new JDKRandomGenerator();
    }

    public static FitResult fit(Table data, ParamSet pars, String engine) {
        FitManager fm = new FitManager();
        XYModel model = new XYModel(Meta.empty(), new GaussianSpectrum());
        FitState state = new FitState(data, model, pars);

        return fm.runStage(state, engine, "run", "pos");
    }

//    public static void printInvHessian(Table data, ParamSet pars) {
//        XYModel model = new XYModel(Meta.empty(),new GaussianSpectrum());
//        FitState fs = FitState.builder().setDataSet(data).setModel(model).build();
//        NamedMatrix h = Hessian.getHessian(fs.getLogLike(), pars, pars.namesAsArray());
//        NamedMatrix hInv = new NamedMatrix(pars.namesAsArray(), MatrixOperations.inverse(h.getMatrix()));
//        FittingIOUtils.printNamedMatrix(System.out, hInv);
//    }

    @Override
    public double derivValue(String parName, double x, Values set) {
        double pos = set.getDouble("pos");
        double w = set.getDouble("w");
        double dif = x - pos;
        switch (parName) {
            case "pos":
                return this.value(x, set) * dif / w / w;
            case "w":
                return value(x, set) / w * (dif * dif / w / w - 1);
            case "amp":
                return value(x, set) / set.getDouble("amp");
            default:
                throw new NameNotFoundException(parName);
        }

    }

    @Override
    public boolean providesDeriv(String name) {
        return this.getNames().contains(name);
    }

    public Table sample(double pos, double w, double amp, double a, double b, int number) {
        ListTable.Builder data = new ListTable.Builder();
        double[] v = new double[3];
        v[0] = w;
        v[1] = pos;
        v[2] = amp;
        NamedVector vector = new NamedVector(list, v);
        double[] grid = GridCalculator.getUniformUnivariateGrid(a, b, number);
        for (double d : grid) {
            double value = this.value(d, vector);
            double error = Math.sqrt(value);
            double randValue = Math.max(0, rnd.nextGaussian() * error + value);
            Values p = Adapters.buildXYDataPoint(d, randValue, Math.max(Math.sqrt(randValue), 1d));
            data.row(p);
        }
        return data.build();
    }

    @Override
    public double value(double x, Values set) {
        double pos = set.getDouble("pos");
        double w = set.getDouble("w");
        double amp = set.getDouble("amp");
        double dif = x - pos;
        return amp * 1 / Math.sqrt(2 * Math.PI) / w * Math.exp(-dif * dif / 2 / w / w);
    }

}
