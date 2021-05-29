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

import hep.dataforge.Named;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.MetaMorph;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main parameter implementation
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class Param implements MetaMorph, Named {

    private String name; //Название параметра
    private double err = Double.NaN;
    private double lowerBound = Double.NEGATIVE_INFINITY; // Область, в которой параметр может существовать.
    private double upperBound = Double.POSITIVE_INFINITY;
    private double value = Double.NaN; //значение параметра

    /**
     * Creates parameter with name.
     *
     * @param str a {@link java.lang.String} object.
     */
    public Param(String str) {
        name = str;
    }

    public Param(String str, double value) {
        this(str);
        this.value = value;
    }

    public Param(){

    }

    public Param(Meta meta){
        name = meta.getString("name", meta.getName());
        setErr(meta.getDouble("err", Double.NaN));
        setDomain(meta.getDouble("lower", Double.NEGATIVE_INFINITY),
                meta.getDouble("upper", Double.POSITIVE_INFINITY));
        setValue(meta.getDouble("value", Double.NaN));
    }

    @NotNull
    @Override
    public Meta toMeta() {
        return new MetaBuilder(getName())
                .setValue("value", getValue())
                .setValue("err",getErr())
                .setValue("lower",getLowerBound())
                .setValue("upper",getUpperBound())
                .build();
    }

    /**
     * Read fir parameter from String using 'name'	= value ± error	(lower,upper)
     * syntax
     *
     * @param str a {@link java.lang.String} object.
     * @return a {@link hep.dataforge.stat.fit.Param} object.
     */
    public static Param fromString(String str) {
        Matcher matcher = Pattern.compile(
                "\\'(?<name>.*)\\'\\s*=*\\s*(?<value>[\\.\\deE]*)\\s*±\\s*(?<error>[\\.\\deE]*)(?:\\s*\\((?<lower>.*),\\s*(?<upper>.*)\\))?"
        ).matcher(str);
        if (matcher.matches()) {
            String name = matcher.group("name");
            double value = Double.valueOf(matcher.group("value"));
            double error = Double.valueOf(matcher.group("error"));
            Param par = new Param(name, value);
            par.setErr(error);
            if (matcher.group("lower") != null && matcher.group("upper") != null) {
                double lower = Double.valueOf(matcher.group("lower"));
                double upper = Double.valueOf(matcher.group("upper"));
                par.setDomain(lower, upper);
            }
            return par;
        } else {
            throw new IllegalArgumentException();
        }

    }

    public Param copy() {
        Param res = new Param(this.name);
        res.value = this.value;
        res.err = this.err;
        res.lowerBound = this.getLowerBound();
        res.upperBound = this.getUpperBound();
        return res;
    }

    public double getErr() {
        return err;
    }

    public void setErr(double error) {
        /*стандартная ошибка или любая другая величина, несущая этот смысл*/
//        if(error<0) throw new CoreException("Error for parameter must be positive.");
        assert error >= 0 : "Error for parameter must be positive.";
        err = error;
    }

    public Double getLowerBound() {
        return lowerBound;
    }

    public Double getUpperBound() {
        return upperBound;
    }

    public String getName() {
        return name;
    }

    public void setDomain(double lower, double upper) {
        /*Метод определяет область параметра, попутно проверяя, что она задана
         правильно*/
        upperBound = upper;
        lowerBound = lower;

        if (getUpperBound() <= getLowerBound()) {
            throw new RuntimeException("Wrong domain.");
        }
    }

    /**
     * Автоматически учитывает границы параметра
     *
     * @param value the value to set
     */
    public void setValue(double value) {
        if (value < this.lowerBound) {
            this.value = lowerBound;
        } else if (value > this.upperBound) {
            this.value = upperBound;
        } else {
            this.value = value;
        }
    }

    public double getValue() {
        return value;
    }

    public boolean isConstrained() {
        return this.lowerBound > Double.NEGATIVE_INFINITY || this.upperBound < Double.POSITIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public String toString() {
        if (Double.isNaN(this.err)) {
            return String.format("'%s'\t= %g", this.name, this.value);
        } else {
            int digits;
            if (value == 0) {
                digits = (int) Math.max(-Math.log10(err) + 1, 1);
            } else {
                digits = (int) (Math.log10(Math.abs(value)));
                digits = digits - (int) (Math.log10(err)) + 3;
            }

            digits = Math.max(digits, 1);

            if (isConstrained()) {
                return String.format("'%s'\t= %." + digits + "g \u00b1 %.2g\t(%g,%g)", this.name, this.value, this.err, this.lowerBound, this.upperBound);
            } else {
                return String.format("'%s'\t= %." + digits + "g \u00b1 %.2g", this.name, this.value, this.err);
            }
        }
    }

}
