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
package hep.dataforge.utils;

/**
 * An utility class providing easy access to Commons Math argument check
 * exceptions
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class ArgumentChecker {

    /**
     * <p>checkEqualDimensions.</p>
     *
     * @param dimensions a int.
     */
    public static void checkEqualDimensions(int... dimensions) {
        if (dimensions.length > 1) {
            for (int i = 1; i < dimensions.length; i++) {
                if (dimensions[i] != dimensions[0]) {
                    throw new IllegalArgumentException();
                }

            }
        }
    }

    /**
     * <p>checkNotNull.</p>
     *
     * @param obj a {@link java.lang.Object} object.
     */
    public static void checkNotNull(Object... obj) {
        if (obj == null) {
            throw new IllegalArgumentException();
        }
        for (Object obj1 : obj) {
            if (obj1 == null) {
                throw new IllegalArgumentException();
            }
        }
    }

}
