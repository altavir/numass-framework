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
package hep.dataforge.MINUIT;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;

/**
 * knows how to andThen between user specified parameters (external) and
 * internal parameters used for minimization
 *
 * Жуткий октопус, который занимается преобразованием внешних параметров во внутренние
 * TODO по возможности отказаться от использования этого монстра
 * @version $Id$
 */
final class MnUserTransformation {
    private static SinParameterTransformation theDoubleLimTrafo = new SinParameterTransformation();
    private static SqrtLowParameterTransformation theLowerLimTrafo = new SqrtLowParameterTransformation();

    private static SqrtUpParameterTransformation theUpperLimTrafo = new SqrtUpParameterTransformation();
    private Map<String, Integer> nameMap = new HashMap<>();
    private List<Double> theCache;
    private List<Integer> theExtOfInt;
    private List<MinuitParameter> theParameters;
    private MnMachinePrecision thePrecision;

    MnUserTransformation() {
        thePrecision = new MnMachinePrecision();
        theParameters = new ArrayList<>();
        theExtOfInt = new ArrayList<>();
        theCache = new ArrayList<>(0);
    }

    private MnUserTransformation(MnUserTransformation other) {
        thePrecision = other.thePrecision;
        theParameters = new ArrayList<>(other.theParameters.size());
        for (MinuitParameter par : other.theParameters) {
            theParameters.add(par.copy());
        }
        theExtOfInt = new ArrayList<>(other.theExtOfInt);
        theCache = new ArrayList<>(other.theCache);
    }

    MnUserTransformation(double[] par, double[] err) {
        thePrecision = new MnMachinePrecision();
        theParameters = new ArrayList<>(par.length);
        theExtOfInt = new ArrayList<>(par.length);
        theCache = new ArrayList<>(par.length);
        for (int i = 0; i < par.length; i++) {
            add("p" + i, par[i], err[i]);
        }
    }

    /**
     * add free parameter
     * @param err
     * @param val
     */
    void add(String name, double val, double err) {
        if (nameMap.containsKey(name)) {
            throw new IllegalArgumentException("duplicate name: " + name);
        }
        nameMap.put(name, theParameters.size());
        theExtOfInt.add(theParameters.size());
        theCache.add(val);
        theParameters.add(new MinuitParameter(theParameters.size(), name, val, err));
    }

    /**
     * add limited parameter
     * @param up
     * @param low
     */
    void add(String name, double val, double err, double low, double up) {
        if (nameMap.containsKey(name)) {
            throw new IllegalArgumentException("duplicate name: " + name);
        }
        nameMap.put(name, theParameters.size());
        theExtOfInt.add(theParameters.size());
        theCache.add(val);
        theParameters.add(new MinuitParameter(theParameters.size(), name, val, err, low, up));
    }

    /**
     * add parameter
     * @param name
     * @param val
     */
    void add(String name, double val) {
        if (nameMap.containsKey(name)) {
            throw new IllegalArgumentException("duplicate name: " + name);
        }
        nameMap.put(name, theParameters.size());
        theCache.add(val);
        theParameters.add(new MinuitParameter(theParameters.size(), name, val));
    }

    /**
     * <p>copy.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserTransformation} object.
     */
    protected MnUserTransformation copy() {
        return new MnUserTransformation(this);
    }

    double dInt2Ext(int i, double val) {
        double dd = 1.;
        MinuitParameter parm = theParameters.get(theExtOfInt.get(i));
        if (parm.hasLimits()) {
            if (parm.hasUpperLimit() && parm.hasLowerLimit()) {
                dd = theDoubleLimTrafo.dInt2Ext(val, parm.upperLimit(), parm.lowerLimit());
            } else if (parm.hasUpperLimit() && !parm.hasLowerLimit()) {
                dd = theUpperLimTrafo.dInt2Ext(val, parm.upperLimit());
            } else {
                dd = theLowerLimTrafo.dInt2Ext(val, parm.lowerLimit());
            }
        }
        
        return dd;
    }

    double error(int index) {
        return theParameters.get(index).error();
    }
    double error(String name) {
        return error(index(name));
    }

    double[] errors() {
        double[] result = new double[theParameters.size()];
        int i = 0;
        for (MinuitParameter parameter : theParameters) {
            result[i++] = parameter.error();
        }
        return result;
    }

    double ext2int(int i, double val) {
        MinuitParameter parm = theParameters.get(i);
        if (parm.hasLimits()) {
            if (parm.hasUpperLimit() && parm.hasLowerLimit()) {
                return theDoubleLimTrafo.ext2int(val, parm.upperLimit(), parm.lowerLimit(), precision());
            } else if (parm.hasUpperLimit() && !parm.hasLowerLimit()) {
                return theUpperLimTrafo.ext2int(val, parm.upperLimit(), precision());
            } else {
                return theLowerLimTrafo.ext2int(val, parm.lowerLimit(), precision());
            }
        }
        
        return val;
    }

    int extOfInt(int internal) {
        return theExtOfInt.get(internal);
    }

    /**
     * interaction via external number of parameter
     * @param index
     */
    void fix(int index) {
        int iind = intOfExt(index);
        theExtOfInt.remove(iind);
        theParameters.get(index).fix();
    }

    /**
     * interaction via name of parameter
     * @param name
     */
    void fix(String name) {
        fix(index(name));
    }

    /**
     * convert name into external number of parameter
     * @param name
     * @return 
     */
    int index(String name) {
        return nameMap.get(name);
    }

    double int2ext(int i, double val) {
        MinuitParameter parm = theParameters.get(theExtOfInt.get(i));
        if (parm.hasLimits()) {
            if (parm.hasUpperLimit() && parm.hasLowerLimit()) {
                return theDoubleLimTrafo.int2ext(val, parm.upperLimit(), parm.lowerLimit());
            } else if (parm.hasUpperLimit() && !parm.hasLowerLimit()) {
                return theUpperLimTrafo.int2ext(val, parm.upperLimit());
            } else {
                return theLowerLimTrafo.int2ext(val, parm.lowerLimit());
            }
        }
        return val;
    }

    MnUserCovariance int2extCovariance(RealVector vec, MnAlgebraicSymMatrix cov) {
        
        MnUserCovariance result = new MnUserCovariance(cov.nrow());
        for (int i = 0; i < vec.getDimension(); i++) {
            double dxdi = 1.;
            if (theParameters.get(theExtOfInt.get(i)).hasLimits()) {
                dxdi = dInt2Ext(i, vec.getEntry(i));
            }
            for (int j = i; j < vec.getDimension(); j++) {
                double dxdj = 1.;
                if (theParameters.get(theExtOfInt.get(j)).hasLimits()) {
                    
                    dxdj = dInt2Ext(j, vec.getEntry(j));
                }
                result.set(i, j, dxdi * cov.get(i, j) * dxdj);
            }
        }
        
        return result;
    }

    double int2extError(int i, double val, double err) {
        double dx = err;
        MinuitParameter parm = theParameters.get(theExtOfInt.get(i));
        if (parm.hasLimits()) {
            double ui = int2ext(i, val);
            double du1 = int2ext(i, val + dx) - ui;
            double du2 = int2ext(i, val - dx) - ui;
            if (parm.hasUpperLimit() && parm.hasLowerLimit()) {
                if (dx > 1.) {
                    du1 = parm.upperLimit() - parm.lowerLimit();
                }
                dx = 0.5 * (Math.abs(du1) + Math.abs(du2));
            } else {
                dx = 0.5 * (Math.abs(du1) + Math.abs(du2));
            }
        }

        return dx;
    }

    int intOfExt(int ext) {
        for (int iind = 0; iind < theExtOfInt.size(); iind++) {
            if (ext == theExtOfInt.get(iind)) {
                return iind;
            }
        }
        throw new IllegalArgumentException("ext=" + ext);
    }

    /**
     * convert external number into name of parameter
     * @param index
     * @return 
     */
    String name(int index) {
        return theParameters.get(index).name();
    }

    /**
     * access to single parameter
     * @param index
     * @return 
     */
    MinuitParameter parameter(int index) {
        return theParameters.get(index);
    }

    List<MinuitParameter> parameters() {
        return theParameters;
    }

    //access to parameters and errors in column-wise representation
    double[] params() {
        double[] result = new double[theParameters.size()];
        int i = 0;
        for (MinuitParameter parameter : theParameters) {
            result[i++] = parameter.value();
        }
        return result;
    }

    MnMachinePrecision precision() {
        return thePrecision;
    }

    void release(int index) {
        if (theExtOfInt.contains(index)) {
            throw new IllegalArgumentException("index=" + index);
        }
        theExtOfInt.add(index);
        Collections.sort(theExtOfInt);
        theParameters.get(index).release();
    }

    void release(String name) {
        release(index(name));
    }

    void removeLimits(int index) {
        theParameters.get(index).removeLimits();
    }

    void removeLimits(String name) {
        removeLimits(index(name));
    }

    void setError(int index, double err) {
        theParameters.get(index).setError(err);
    }

    void setError(String name, double err) {
        setError(index(name), err);
    }

    void setLimits(int index, double low, double up) {
        theParameters.get(index).setLimits(low, up);
    }

    void setLimits(String name, double low, double up) {
        setLimits(index(name), low, up);
    }

    void setLowerLimit(int index, double low) {
        theParameters.get(index).setLowerLimit(low);
    }

    void setLowerLimit(String name, double low) {
        setLowerLimit(index(name), low);
    }

    void setPrecision(double eps) {
        thePrecision.setPrecision(eps);
    }

    void setUpperLimit(int index, double up) {
        theParameters.get(index).setUpperLimit(up);
    }

    void setUpperLimit(String name, double up) {
        setUpperLimit(index(name), up);
    }

    void setValue(int index, double val) {
        theParameters.get(index).setValue(val);
        theCache.set(index, val);
    }

    void setValue(String name, double val) {
        setValue(index(name), val);
    }

    ArrayRealVector transform(RealVector pstates) {
        // FixMe: Worry about efficiency here
        ArrayRealVector result = new ArrayRealVector(theCache.size());
        for (int i = 0; i < result.getDimension(); i++) {
            result.setEntry(i, theCache.get(i));
        }
        for (int i = 0; i < pstates.getDimension(); i++) {
            if (theParameters.get(theExtOfInt.get(i)).hasLimits()) {
                result.setEntry(theExtOfInt.get(i), int2ext(i, pstates.getEntry(i)));
            } else {
                result.setEntry(theExtOfInt.get(i), pstates.getEntry(i));
            }
        }
        return result;
    }
    //forwarded interface
    
    double value(int index) {
        return theParameters.get(index).value();
    }

    double value(String name) {
        return value(index(name));
    }

    int variableParameters() {
        return theExtOfInt.size();
    }
}
