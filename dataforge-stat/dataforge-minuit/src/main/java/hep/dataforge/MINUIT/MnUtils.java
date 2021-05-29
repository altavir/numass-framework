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

/**
 * Utilities for operating on vectors and matrices
 *
 * @version $Id$
 */
abstract class MnUtils {

    static double absoluteSumOfElements(MnAlgebraicSymMatrix m) {
        double[] data = m.data();
        double result = 0;
        for (int i = 0; i < data.length; i++) {
            result += Math.abs(data[i]);
        }
        return result;
    }

    static RealVector add(RealVector v1, RealVector v2) {
        return v1.add(v2);
    }

    static MnAlgebraicSymMatrix add(MnAlgebraicSymMatrix m1, MnAlgebraicSymMatrix m2) {
        if (m1.size() != m2.size()) {
            throw new IllegalArgumentException("Incompatible matrices");
        }
        MnAlgebraicSymMatrix result = m1.copy();
        double[] a = result.data();
        double[] b = m2.data();
        for (int i = 0; i < a.length; i++) {
            a[i] += b[i];
        }
        return result;
    }

    static MnAlgebraicSymMatrix div(MnAlgebraicSymMatrix m, double scale) {
        return mul(m, 1 / scale);
    }

    static RealVector div(RealVector m, double scale) {
        return mul(m, 1 / scale);
    }

    static double innerProduct(RealVector v1, RealVector v2) {
        if (v1.getDimension() != v2.getDimension()) {
            throw new IllegalArgumentException("Incompatible vectors");
        }
        double total = 0;
        for (int i = 0; i < v1.getDimension(); i++) {
            total += v1.getEntry(i)*v2.getEntry(i);
        }
        return total;
    }

    static RealVector mul(RealVector v1, double scale) {
        return v1.mapMultiply(scale);        
    }

    static MnAlgebraicSymMatrix mul(MnAlgebraicSymMatrix m1, double scale) {
        MnAlgebraicSymMatrix result = m1.copy();
        double[] a = result.data();
        for (int i = 0; i < a.length; i++) {
            a[i] *= scale;
        }
        return result;
    }

    static ArrayRealVector mul(MnAlgebraicSymMatrix m1, RealVector v1) {
        if (m1.nrow() != v1.getDimension()) {
            throw new IllegalArgumentException("Incompatible arguments");
        }
        ArrayRealVector result = new ArrayRealVector(m1.nrow());
        for (int i = 0; i < result.getDimension(); i++) {
            double total = 0;
            for (int k = 0; k < result.getDimension(); k++) {
                total += m1.get(i, k) * v1.getEntry(k);
            }
            result.setEntry(i, total);
        }
        return result;
    }

    static MnAlgebraicSymMatrix mul(MnAlgebraicSymMatrix m1, MnAlgebraicSymMatrix m2) {
        if (m1.size() != m2.size()) {
            throw new IllegalArgumentException("Incompatible matrices");
        }
        int n = m1.nrow();
        MnAlgebraicSymMatrix result = new MnAlgebraicSymMatrix(n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double total = 0;
                for (int k = 0; k < n; k++) {
                    total += m1.get(i, k) * m2.get(k, j);
                }
                result.set(i, j, total);
            }
        }
        return result;
    }

    static MnAlgebraicSymMatrix outerProduct(RealVector v2) {
        // Fixme: check this. I am assuming this is just an outer-product of vector
        //        with itself.
        int n = v2.getDimension();
        MnAlgebraicSymMatrix result = new MnAlgebraicSymMatrix(n);
        double[] data = v2.toArray();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                result.set(i, j, data[i] * data[j]);
            }
        }
        return result;
    }

    static double similarity(RealVector avec, MnAlgebraicSymMatrix mat) {
        int n = avec.getDimension();
        RealVector tmp = mul(mat, avec);
        double result = 0;
        for (int i = 0; i < n; i++) {
            result += tmp.getEntry(i) * avec.getEntry(i);
        }
        return result;
    }

    static RealVector sub(RealVector v1, RealVector v2) {
        return v1.subtract(v2);
    }

    static MnAlgebraicSymMatrix sub(MnAlgebraicSymMatrix m1, MnAlgebraicSymMatrix m2) {
        if (m1.size() != m2.size()) {
            throw new IllegalArgumentException("Incompatible matrices");
        }
        MnAlgebraicSymMatrix result = m1.copy();
        double[] a = result.data();
        double[] b = m2.data();
        for (int i = 0; i < a.length; i++) {
            a[i] -= b[i];
        }
        return result;
    }
}
