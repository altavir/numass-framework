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
package hep.dataforge.maths;

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.exceptions.NamingException;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.MetaMorph;
import hep.dataforge.names.NameList;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueFactory;
import hep.dataforge.values.Values;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A {@link Values} implementation wrapping Commons Math {@link RealVector}
 *
 * @author Alexander Nozik
 */
public class NamedVector implements Values, MetaMorph {

    private NameList nameList;
    private RealVector vector;

    /**
     * Serialization constructor
     */
    public NamedVector() {
        nameList = new NameList();
        vector = new ArrayRealVector();
    }

    public NamedVector(NameList nameList, RealVector vector) {
        this.nameList = nameList;
        this.vector = vector;
    }

    public NamedVector(String[] names, RealVector v) {
        if (names.length != v.getDimension()) {
            throw new IllegalArgumentException();
        }
        vector = new ArrayRealVector(v);
        this.nameList = new NameList(names);
    }

    public NamedVector(String[] names, double[] d) {
        if (names.length != d.length) {
            throw new DimensionMismatchException(d.length, names.length);
        }
        vector = new ArrayRealVector(d);
        this.nameList = new NameList(names);
    }

    public NamedVector(NameList names, double[] d) {
        if (names.size() != d.length) {
            throw new DimensionMismatchException(d.length, names.size());
        }
        vector = new ArrayRealVector(d);
        this.nameList = new NameList(names);
    }

    public NamedVector(Values set) {
        vector = new ArrayRealVector(MathUtils.getDoubleArray(set));
        this.nameList = new NameList(set.getNames());
    }

    public NamedVector(Meta meta) {
        nameList = new NameList(meta.getValueNames());
        double[] values = new double[nameList.size()];
        for (int i = 0; i < nameList.size(); i++) {
            values[i] = meta.getDouble(nameList.get(i));
        }
        this.vector = new ArrayRealVector(values);
    }

    @Override
    public Optional<Value> optValue(@NotNull String path) {
        int n = this.getNumberByName(path);
        if (n < 0) {
            return Optional.empty();
        } else {
            return Optional.of(ValueFactory.of(vector.getEntry(n)));
        }
    }

    public NamedVector copy() {
        return new NamedVector(this.namesAsArray(), vector);
    }


    /**
     * {@inheritDoc}
     *
     * @param name
     */
    @Override
    public double getDouble(String name) {
        int n = this.getNumberByName(name);
        if (n < 0) {
            throw new NameNotFoundException(name);
        }
        return vector.getEntry(n);
    }

    public int getNumberByName(String name) {
        return nameList.getNumberByName(name);
    }


    public NamedVector subVector(String... names) {
        if (names.length == 0) {
            return this;
        }
        return new NamedVector(names, getArray(names));
    }

    /**
     * {@inheritDoc}
     */
    public double[] getArray(String... names) {
        if (names.length == 0) {
            return vector.toArray();
        } else {
            if (!this.getNames().contains(names)) {
                throw new NamingException();
            }
            double[] res = new double[names.length];
            for (int i = 0; i < names.length; i++) {
                res[i] = vector.getEntry(this.getNumberByName(names[i]));
            }
            return res;
        }
    }

    public RealVector getVector() {
        return vector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NameList getNames() {
        return nameList;
    }

    public void setValue(String name, double val) {
        int n = this.getNumberByName(name);
        if (n < 0) {
            throw new NameNotFoundException(name);
        }

        vector.setEntry(n, val);
    }

    @NotNull
    @Override
    public Meta toMeta() {
        MetaBuilder builder = new MetaBuilder("vector");
        for (int i = 0; i < getNames().size(); i++) {
            builder.setValue(nameList.get(i), vector.getEntry(i));
        }
        return builder;
    }
}
