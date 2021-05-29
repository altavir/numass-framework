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
 *
 * @version $Id$
 */
class VariableMetricMinimizer extends ModularFunctionMinimizer {

    private VariableMetricBuilder theMinBuilder;
    private MnSeedGenerator theMinSeedGen;

    /**
     * <p>Constructor for VariableMetricMinimizer.</p>
     */
    public VariableMetricMinimizer() {
        theMinSeedGen = new MnSeedGenerator();
        theMinBuilder = new VariableMetricBuilder();
    }

    /** {@inheritDoc} */
    @Override
    public MinimumBuilder builder() {
        return theMinBuilder;
    }

        /** {@inheritDoc} */
    @Override
    public MinimumSeedGenerator seedGenerator() {
        return theMinSeedGen;
    }
}
