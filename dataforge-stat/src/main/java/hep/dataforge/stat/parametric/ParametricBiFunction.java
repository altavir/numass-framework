/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.stat.parametric;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.names.NameList;
import hep.dataforge.names.NameSetContainer;
import hep.dataforge.values.Values;

/**
 *
 * @author Alexander Nozik
 */
public interface ParametricBiFunction extends NameSetContainer {

    double derivValue(String parName, double x, double y, Values set);

    double value(double x, double y, Values set);

    boolean providesDeriv(String name);

    default ParametricBiFunction derivative(String parName) {
        if (providesDeriv(parName)) {
            return new ParametricBiFunction() {
                @Override
                public double derivValue(String parName, double x, double y, Values set) {
                    throw new NotDefinedException();
                }

                @Override
                public double value(double x, double y, Values set) {
                    return ParametricBiFunction.this.derivValue(parName, x, y, set);
                }

                @Override
                public boolean providesDeriv(String name) {
                    return !getNames().contains(name);
                }

                @Override
                public NameList getNames() {
                    return ParametricBiFunction.this.getNames();
                }
            };
        } else {
            throw new NotDefinedException();
        }
    }
}
