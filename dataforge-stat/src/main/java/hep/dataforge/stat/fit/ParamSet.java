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
package hep.dataforge.stat.fit;

import hep.dataforge.description.NodeDef;
import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.maths.NamedVector;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.MetaMorph;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.names.NameList;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueFactory;
import hep.dataforge.values.Values;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * FIXME fix documentation
 * Реализация набора параметров, которая будет потом использоваться в Result,
 * Fitter и Spectrum
 * <p>
 * Подразумевается, что ParamSet обязательно содержит помимо значения хотя бы
 * ошибку.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class ParamSet implements Values, MetaMorph {

    private final HashMap<String, Param> params = new LinkedHashMap<>();

    /**
     * Generates set of parameters with predefined names.
     *
     * @param names an array of {@link java.lang.String} objects.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    public ParamSet(String[] names) throws NameNotFoundException {
        int num = names.length;
        int i, j;

        for (i = 0; i < num - 1; i++) { //Проверяем, нет ли совпадающих имен
            for (j = i + 1; j < num; j++) {
                if (names[i].equals(names[j])) {
                    throw new NameNotFoundException("ParamSet naming error: Names are not unique");
                }
            }
        }

        for (i = 0; i < num; i++) {
            this.params.put(names[i], new Param(names[i]));
        }
    }

    public ParamSet(ParamSet other) {
        for (Param par : other.getParams()) {
            params.put(par.getName(), par.copy());
        }
    }

    public ParamSet() {
    }

    public ParamSet(Values values) {
        for (String name : values.getNames()) {
            this.params.put(name, new Param(name, values.getDouble(name)));
        }
    }

    @NodeDef(key = "params", info = "Used as a wrapper for 'param' elements.")
    public ParamSet(Meta meta){

        Meta params;
        if (meta.hasMeta("params")) {
            params = meta.getMeta("params");
        } else if ("params".equals(meta.getName())) {
            params = meta;
        } else {
            return;
        }

        MetaUtils.nodeStream(params).forEach(entry -> {
            setPar(new Param(entry.getSecond()));
        });
    }

    @NotNull
    @Override
    public Meta toMeta() {
        MetaBuilder builder = new MetaBuilder("params");
        params.values().forEach(param -> builder.putNode(param.toMeta()));
        return builder.build();
    }

    /**
     * Read parameter set from lines using 'name'	= value ± error	(lower,upper)
     * syntax
     *
     * @param str a {@link java.lang.String} object.
     * @return a {@link hep.dataforge.stat.fit.ParamSet} object.
     */
    public static ParamSet fromString(String str) {
        Scanner scan = new Scanner(str);
        ParamSet set = new ParamSet();
        while (scan.hasNextLine()) {
            set.setPar(Param.fromString(scan.nextLine()));
        }
        return set;
    }

    @NotNull
    @Override
    public Optional<Value> optValue(@NotNull String path) {
        return optByName(path).map(par -> ValueFactory.of(par.getValue()));
    }


    public ParamSet copy() {
        return new ParamSet(this);
    }

    /**
     * Returns link to parameter with specific name. Возвращает параметр по его
     * имени.
     *
     * @param str a {@link java.lang.String} object.
     * @return null if name is not found.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    public Param getByName(String str) throws NameNotFoundException {
        Param res = this.params.get(str);
        if (res != null) {
            return res;
        } else {
            throw new NameNotFoundException(str);
        }
    }

    public Optional<Param> optByName(String str) {
        return Optional.ofNullable(this.params.get(str));
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return params.size();
    }

    public double getError(String str) throws NameNotFoundException {
        Param P;
        P = this.getByName(str);
        return P.getErr();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public NameList getNames() {
        return new NameList(this.params.keySet());
    }

    /**
     * <p>
     * getParErrors.</p>
     *
     * @param names a {@link java.lang.String} object.
     * @return a {@link hep.dataforge.maths.NamedVector} object.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    public NamedVector getParErrors(String... names) throws NameNotFoundException {
        if (names.length == 0) {
            names = this.namesAsArray();
        }
        assert this.getNames().contains(names);

        double[] res = new double[names.length];

        for (int i = 0; i < res.length; i++) {
            res[i] = this.getError(names[i]);

        }
        return new NamedVector(names, res);
    }

    /**
     * <p>
     * getParValues.</p>
     *
     * @param names a {@link java.lang.String} object.
     * @return a {@link hep.dataforge.maths.NamedVector} object.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    public NamedVector getParValues(String... names) throws NameNotFoundException {
        if (names.length == 0) {
            names = this.namesAsArray();
        }
        assert this.getNames().contains(names);

        double[] res = new double[names.length];

        for (int i = 0; i < res.length; i++) {
            res[i] = this.getDouble(names[i]);

        }
        return new NamedVector(names, res);
    }

    /**
     * <p>
     * Getter for the field <code>params</code>.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    public Collection<Param> getParams() {
        return params.values();
    }

    /**
     * Returns a parameter set witch consists only of names presented as
     * parameter (values are also copied).
     *
     * @param names a {@link java.lang.String} object.
     * @return a {@link hep.dataforge.stat.fit.ParamSet} object.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    public ParamSet getSubSet(String... names) throws NameNotFoundException {
        if (names.length == 0) {
            return this.copy();
        }
        int i;
        ParamSet res = new ParamSet(names);
        for (i = 0; i < names.length; i++) {
            res.params.put(names[i], this.getByName(names[i]).copy());
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Метод возвращает значение параметра с именем str
     *
     * @param str
     */
    @Override
    public double getDouble(String str) throws NameNotFoundException {
        Param p;
        p = this.getByName(str);
        return p.getValue();
    }

    /**
     * {@inheritDoc}
     */
    public double[] getArray(String... names) {
        return this.getParValues(names).getArray();
    }

    /**
     * Searches set for a parameter with the same name and replaces it. Only
     * link is replaced, use {@code copy} to make a deep copy.
     * <p>
     * In case name not found adds a new parameter
     *
     * @param input a {@link hep.dataforge.stat.fit.Param} object.
     * @return a {@link hep.dataforge.stat.fit.ParamSet} object.
     */
    public ParamSet setPar(Param input) {
        this.params.put(input.getName(), input);
        return this;
    }


    private ParamSet upadatePar(String name, Consumer<Param> consumer) {
        Param par;
        if (!params.containsKey(name)) {
            LoggerFactory.getLogger(getClass())
                    .trace("Parameter with name '{}' not found. Adding a new parameter with this name.", name);
            par = new Param(name);
            this.params.put(name, par);
        } else {
            par = getByName(name);
        }
        consumer.accept(par);
        return this;
    }

    public ParamSet setPar(String name, double value, double error) {
        return upadatePar(name, (par) -> {
            par.setValue(value);
            par.setErr(error);
        });
    }

    public ParamSet setPar(String name, double value, double error, Double lower, Double upper) {
        return upadatePar(name, (par) -> {
            par.setValue(value);
            par.setErr(error);
            par.setDomain(lower, upper);
        });
    }

    public ParamSet setParValue(String name, double value) {
        return upadatePar(name, (par) -> {
            par.setValue(value);
        });
    }

    public ParamSet setParDomain(String name, Double lower, Double upper) throws NameNotFoundException {
        Param Par;
        Par = getByName(name);

        Par.setDomain(lower, upper);
        return this;
    }

    public ParamSet setParError(String name, double value) throws NameNotFoundException {
        Param Par;
        Par = getByName(name);
        Par.setErr(value);
        return this;
    }

    /**
     * method to set all parameter errors.
     *
     * @param errors
     * @return a {@link hep.dataforge.stat.fit.ParamSet} object.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    public ParamSet setParErrors(Values errors) throws NameNotFoundException {
        if (!this.getNames().contains(errors.getNames())) {
            throw new NameNotFoundException();
        }
        for (String name : errors.getNames()) {
            this.setParError(name, errors.getDouble(name));
        }
        return this;
    }

    /**
     * method to set all parameter values.
     *
     * @param values
     * @return a {@link hep.dataforge.stat.fit.ParamSet} object.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    public ParamSet setParValues(Values values) throws NameNotFoundException {
        if (!this.getNames().contains(values.getNames())) {
            throw new NameNotFoundException();
        }
        int i;
        for (String name : values.getNames()) {
            this.setParValue(name, values.getDouble(name));
        }
        return this;
    }

    /**
     * <p>
     * updateFrom.</p>
     *
     * @param set a {@link hep.dataforge.stat.fit.ParamSet} object.
     */
    public void updateFrom(ParamSet set) {
        set.getParams().forEach(this::setPar);
    }

}
