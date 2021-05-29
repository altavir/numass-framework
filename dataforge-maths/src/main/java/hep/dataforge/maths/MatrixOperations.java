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

import hep.dataforge.context.Global;
import org.apache.commons.math3.linear.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static java.lang.Math.abs;
import static java.util.Arrays.fill;
import static java.util.Arrays.sort;

/**
 * <p>MatrixOperations class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class MatrixOperations {

    /**
     * Constant <code>MATRIX_SINGULARITY_THRESHOLD="maths.singularity_threshold"</code>
     */
    public static String MATRIX_SINGULARITY_THRESHOLD = "maths.singularity_threshold";

    /**
     * <p>condCheck.</p>
     *
     * @param matrix a {@link org.apache.commons.math3.linear.RealMatrix} object.
     * @return a boolean.
     */
    public static boolean condCheck(RealMatrix matrix) {
        double det = determinant(matrix);
        if (det == 0) {
            return false;
        }
        double det1 = determinant(inverse(matrix));
        return abs(det * det1 - 1) < Global.INSTANCE.getDouble("CONDITIONALITY", 1e-4);
    }

    /**
     * <p>determinant.</p>
     *
     * @param matrix a {@link org.apache.commons.math3.linear.RealMatrix} object.
     * @return a double.
     */
    public static double determinant(RealMatrix matrix) {
        LUDecomposition solver = new LUDecomposition(matrix);
        return solver.getDeterminant();
    }

    /**
     * Обращение положительно определенной квадратной матрицы. В случае если
     * матрица сингулярная, предпринимается попытка регуляризовать ее
     * добавлением небольшой величины к диагонали
     *
     * @param matrix a {@link org.apache.commons.math3.linear.RealMatrix} object.
     * @return a {@link org.apache.commons.math3.linear.RealMatrix} object.
     */
    public static RealMatrix inverse(RealMatrix matrix) {
        Logger logger = LoggerFactory.getLogger(MatrixOperations.class);
        assert matrix.getColumnDimension() == matrix.getRowDimension();
        RealMatrix res;
        try {
            double singularityThreshold = Global.INSTANCE.getDouble(MATRIX_SINGULARITY_THRESHOLD, 1e-11);
            DecompositionSolver solver = new LUDecomposition(matrix, singularityThreshold).getSolver();
            res = solver.getInverse();
        } catch (SingularMatrixException ex) {
            EigenDecomposition eigen = new EigenDecomposition(matrix);
            logger.info("MatrixUtils : Matrix inversion failed. Trying to regulirize matrix by adding a constant to diagonal.");
            double[] eigenValues = eigen.getRealEigenvalues();

            logger.info("MatrixUtils : The eigenvalues are {}", Arrays.toString(eigenValues));
            sort(eigenValues);
            double delta = 0;
            //Во-первых устраняем отрицательные собственные значения, так как предполагается, что матрица положительно определена

            if (eigenValues[0] < 0) {
                delta = -eigenValues[0];
            }
            /*задаем дельту так, чтобы она была заведомо меньще самого большого собственного значения.
             * Цифра 1/1000 взята из минуита.
             */
            delta += (eigenValues[eigenValues.length - 1] - delta) / 1000;
            logger.info("MatrixUtils : Adding {} to diagonal.", delta);
            double[] e = new double[matrix.getColumnDimension()];
            fill(e, delta);
            RealMatrix newMatrix = matrix.add(new DiagonalMatrix(e));
            eigen = new EigenDecomposition(newMatrix);
            DecompositionSolver solver = eigen.getSolver();
            res = solver.getInverse();
        }

        return res;
    }

    /**
     * <p>matrixToSciLab.</p>
     *
     * @param mat a {@link org.apache.commons.math3.linear.RealMatrix} object.
     * @return a {@link java.lang.String} object.
     */
    public static String matrixToSciLab(RealMatrix mat) {
        StringBuilder str = new StringBuilder();
        str.append("[");
        for (int i = 0; i < mat.getRowDimension(); i++) {
            for (int j = 0; j < mat.getColumnDimension(); j++) {
                str.append(mat.getEntry(i, j));
                if (j < mat.getColumnDimension() - 1) {
                    str.append(",");
                }
            }
            if (i < mat.getColumnDimension() - 1) {
                str.append(";");
            }
        }
        str.append("]");
        return str.toString();
    }

    /**
     * <p>isSquareArray.</p>
     *
     * @param arr an array of double.
     * @return a boolean.
     */
    public static boolean isSquareArray(double[][] arr) {
        for (double[] arr1 : arr) {
            if (arr1.length != arr.length) {
                return false;
            }
        }
        return true;
    }
}
