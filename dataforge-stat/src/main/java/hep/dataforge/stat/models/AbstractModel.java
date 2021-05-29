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
package hep.dataforge.stat.models;

import hep.dataforge.exceptions.NamingException;
import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaHolder;
import hep.dataforge.names.NameList;
import hep.dataforge.stat.parametric.AbstractParametricValue;
import hep.dataforge.stat.parametric.ParametricValue;
import hep.dataforge.tables.ValuesAdapter;
import hep.dataforge.values.Values;

/**
 * Basic implementation for model
 *
 * @author Alexander Nozik
 */
public abstract class AbstractModel extends MetaHolder implements Model {
//TODO add default parameters to model

    private final NameList names;

    /**
     *
     */
    protected ValuesAdapter adapter;

    protected AbstractModel(Meta meta, NameList names, ValuesAdapter adapter) {
        super(meta);
        this.adapter = adapter;
        this.names = names;
    }

//    protected AbstractModel(Meta meta, NameSetContainer source, T adapter) {
//        this(meta, source.getNames(), adapter);
//    }

    public ValuesAdapter getAdapter() {
        return adapter;
    }

    public final void setAdapter(ValuesAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * {@inheritDoc}
     */
    public int getDimension() {
        return names.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParametricValue getDistanceFunction(Values point) {
        return new AbstractParametricValue(names) {

            @Override
            public double derivValue(String derivParName, Values pars) throws NotDefinedException, NamingException {
                return disDeriv(derivParName, point, pars);
            }

            @Override
            public boolean providesDeriv(String name) {
                return AbstractModel.this.providesDeriv(name);
            }

            @Override
            public double value(Values pars) throws NamingException {
                return distance(point, pars);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParametricValue getLogProbFunction(Values point) {
        if (!providesProb()) {
            throw new IllegalStateException("Model does not provide internal probability distribution");
        }

        return new AbstractParametricValue(names) {
            @Override
            public double derivValue(String derivParName, Values pars) throws NotDefinedException, NamingException {
                return getLogProbDeriv(derivParName, point, pars);
            }

            @Override
            public boolean providesDeriv(String name) {
                return AbstractModel.this.providesProbDeriv(name);
            }

            @Override
            public double value(Values pars) throws NamingException {
                return getLogProb(point, pars);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NameList getNames() {
        return names;
    }
}
