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
package hep.dataforge.maths.domains;

import hep.dataforge.exceptions.NotDefinedException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * n-dimensional volume
 *
 * @author Alexander Nozik
 */
public interface Domain {

    boolean contains(RealVector point);

    default boolean contains(double[] point) {
        return this.contains(new ArrayRealVector(point));
    }

    default boolean contains(Double[] point) {
        return this.contains(new ArrayRealVector(point));
    }

    RealVector nearestInDomain(RealVector point);

    /**
     * The lower edge for the domain going down from point
     * @param num
     * @param point
     * @return
     */
    Double getLowerBound(int num, RealVector point);

    /**
     * The upper edge of the domain going up from point
     * @param num
     * @param point
     * @return
     */
    Double getUpperBound(int num, RealVector point);

    /**
     * Global lower edge
     * @param num
     * @return
     * @throws NotDefinedException
     */
    Double getLowerBound(int num) throws NotDefinedException;

    /**
     * Global upper edge
     * @param num
     * @return
     * @throws NotDefinedException
     */
    Double getUpperBound(int num) throws NotDefinedException;

    /**
     * Hyper volume
     * @return
     */
    double volume();

    /**
     * Number of Hyperspace dimensions
     * @return
     */
    int getDimension();

//    /**
//     * <p>isFixed.</p>
//     *
//     * @param num a int.
//     * @return a boolean.
//     */
//    boolean isFixed(int num);
}
