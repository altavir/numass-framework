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
package hep.dataforge.maths.integration;

/**
 * An Integrand keeps a function to integrate, borders and the history of
 * integration. Additionally it keeps any integrator specific transitive data.
 *
 * The Integrand supposed to be immutable to support functional-style
 * programming
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public interface Integrand {

    /**
     * The current calculated value. equals Double.NaN if no successful
     * iterations were made so far
     *
     * @return a {@link java.lang.Double} object.
     */
    Double getValue();

    /**
     * the number of calls of function
     *
     * @return a int.
     */
    int getNumCalls();

    /**
     * the dimension of function
     *
     * @return a int.
     */
    int getDimension();
}
