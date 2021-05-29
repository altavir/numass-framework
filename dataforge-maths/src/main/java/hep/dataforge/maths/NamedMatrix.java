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

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.MetaMorph;
import hep.dataforge.names.NameList;
import hep.dataforge.names.NameSetContainer;
import hep.dataforge.values.Values;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * Square named matrix.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class NamedMatrix implements NameSetContainer, MetaMorph {

    private NameList names;
    private RealMatrix mat;

    public NamedMatrix() {
        names = new NameList();
        mat = new Array2DRowRealMatrix();
    }

    public NamedMatrix(String[] names, RealMatrix mat) {
        this.names = new NameList(names);
        if (!mat.isSquare()) {
            throw new IllegalArgumentException("Only square matrices allowed.");
        }
        if (mat.getColumnDimension() != names.length) {
            throw new DimensionMismatchException(mat.getColumnDimension(), names.length);
        }
        this.mat = new Array2DRowRealMatrix(mat.getData(), true);
    }

    public NamedMatrix(String[] names, double[][] values) {
        this.names = new NameList(names);
        if (values.length != values[0].length) {
            throw new IllegalArgumentException("Only square matrices allowed.");
        }
        if (values.length != names.length) {
            throw new DimensionMismatchException(values.length, names.length);
        }
        this.mat = new Array2DRowRealMatrix(values, true);
    }

    public NamedMatrix(Meta meta){
        Map<String, NamedVector> vectors = new HashMap<>();
        meta.getNodeNames().forEach(name -> {
            vectors.put(name, new NamedVector(meta.getMeta(name)));
        });
        this.names = new NameList(vectors.keySet());
        this.mat = new Array2DRowRealMatrix(names.size(), names.size());
        for (int i = 0; i < names.size(); i++) {
            mat.setRowVector(i, vectors.get(names.get(i)).getVector());
        }
    }

    /**
     * Create diagonal named matrix from given named double set
     *
     * @param vector
     * @return
     */
    public static NamedMatrix diagonal(Values vector) {
        double[] vectorValues = MathUtils.getDoubleArray(vector);
        double[][] values = new double[vectorValues.length][vectorValues.length];
        for (int i = 0; i < vectorValues.length; i++) {
            values[i][i] = vectorValues[i];
        }
        return new NamedMatrix(vector.namesAsArray(), values);
    }

    public NamedMatrix copy() {
        return new NamedMatrix(this.namesAsArray(), getMatrix().copy());
    }

    public double get(int i, int j) {
        return mat.getEntry(i, j);
    }

    public double get(String name1, String name2) {
        return mat.getEntry(this.names.getNumberByName(name1), this.names.getNumberByName(name2));
    }

    public RealMatrix getMatrix() {
        return this.mat;
    }

    /**
     * Return named submatrix with given names. The order of names in submatrix
     * is the one provided by arguments. If name list is empty, return this.
     *
     * @param names a {@link java.lang.String} object.
     * @return a {@link hep.dataforge.maths.NamedMatrix} object.
     */
    public NamedMatrix subMatrix(String... names) {
        if (names.length == 0) {
            return this;
        }
        if (!this.getNames().contains(names)) {
            throw new IllegalArgumentException();
        }
        int[] numbers = new int[names.length];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = this.names.getNumberByName(names[i]);

        }
        RealMatrix newMat = this.mat.getSubMatrix(numbers, numbers);
        return new NamedMatrix(names, newMat);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public NameList getNames() {
        return names;
    }

    public void setElement(String name1, String name2, double value) {
        mat.setEntry(this.names.getNumberByName(name1), this.names.getNumberByName(name2), value);
    }

    /**
     * update values of this matrix from corresponding values of given named
     * matrix. The order of columns does not matter.
     *
     * @param matrix a {@link hep.dataforge.maths.NamedMatrix} object.
     */
    public void setValuesFrom(NamedMatrix matrix) {
        for (int i = 0; i < matrix.getNames().size(); i++) {
            for (int j = 0; j < matrix.getNames().size(); j++) {
                String name1 = matrix.names.get(i);
                String name2 = matrix.names.get(j);
                if (names.contains(name1) && names.contains(name2)) {
                    this.setElement(name1, name2, matrix.get(i, j));
                }
            }

        }
    }

    public NamedVector getRow(String name) {
        return new NamedVector(names, getMatrix().getRowVector(names.getNumberByName(name)));
    }

    public NamedVector getColumn(String name) {
        return new NamedVector(names, getMatrix().getColumnVector(names.getNumberByName(name)));
    }

    @Override
    public Meta toMeta() {
        //Serialisator in fact works for non-square matrices
        MetaBuilder res = new MetaBuilder("matrix");
        for (int i = 0; i < mat.getRowDimension(); i++) {
            String name = names.get(i);
            res.putNode(name, getRow(name).toMeta());
        }
        return res;
    }

}
