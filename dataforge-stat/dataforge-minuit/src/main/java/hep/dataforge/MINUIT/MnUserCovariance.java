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

/**
 * MnUserCovariance is the external covariance matrix designed for the
 * interaction of the user. The result of the minimization (internal covariance
 * matrix) is converted into the user representable format. It can also be used
 * as input prior to the minimization. The size of the covariance matrix is
 * according to the number of variable parameters (free and limited).
 *
 * @version $Id$
 * @author Darksnake
 */
public class MnUserCovariance {

    private double[] theData;
    private int theNRow;

    private MnUserCovariance(MnUserCovariance other) {
        theData = other.theData.clone();
        theNRow = other.theNRow;
    }

    MnUserCovariance() {
        theData = new double[0];
        theNRow = 0;
    }
    /*
     * covariance matrix is stored in upper triangular packed storage format,
     * e.g. the elements in the array are arranged like
     * {a(0,0), a(0,1), a(1,1), a(0,2), a(1,2), a(2,2), ...},
     * the size is nrow*(nrow+1)/2.
     */

    MnUserCovariance(double[] data, int nrow) {
        if (data.length != nrow * (nrow + 1) / 2) {
            throw new IllegalArgumentException("Inconsistent arguments");
        }
        theData = data;
        theNRow = nrow;
    }

    /**
     * <p>Constructor for MnUserCovariance.</p>
     *
     * @param nrow a int.
     */
    public MnUserCovariance(int nrow) {
        theData = new double[nrow * (nrow + 1) / 2];
        theNRow = nrow;
    }

    /**
     * <p>copy.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     */
    protected MnUserCovariance copy() {
        return new MnUserCovariance(this);
    }

    double[] data() {
        return theData;
    }

    /**
     * <p>get.</p>
     *
     * @param row a int.
     * @param col a int.
     * @return a double.
     */
    public double get(int row, int col) {
        if (row >= theNRow || col >= theNRow) {
            throw new IllegalArgumentException();
        }
        if (row > col) {
            return theData[col + row * (row + 1) / 2];
        } else {
            return theData[row + col * (col + 1) / 2];
        }
    }

    /**
     * <p>ncol.</p>
     *
     * @return a int.
     */
    public int ncol() {
        return theNRow;
    }

    /**
     * <p>nrow.</p>
     *
     * @return a int.
     */
    public int nrow() {
        return theNRow;
    }

    void scale(double f) {
        for (int i = 0; i < theData.length; i++) {
            theData[i] *= f;
        }
    }

    /**
     * <p>set.</p>
     *
     * @param row a int.
     * @param col a int.
     * @param value a double.
     */
    public void set(int row, int col, double value) {
        if (row >= theNRow || col >= theNRow) {
            throw new IllegalArgumentException();
        }
        if (row > col) {
            theData[col + row * (row + 1) / 2] = value;
        } else {
            theData[row + col * (col + 1) / 2] = value;
        }
    }

    int size() {
        return theData.length;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MnPrint.toString(this);
    }
}
