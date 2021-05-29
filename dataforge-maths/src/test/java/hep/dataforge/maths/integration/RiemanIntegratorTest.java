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

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Alexander Nozik
 */
public class RiemanIntegratorTest {

    static UnivariateFunction gausss = (e) -> 1d/Math.sqrt(2*Math.PI)*Math.exp(-(e*e)/2);
    
    public RiemanIntegratorTest() {
    }

    /**
     * Test of evaluate method, of class RiemanIntegrator.
     */
    @Test
    public void testIntegration() {
        System.out.println("integration test with simple Rieman integrator");
        assertEquals(1d, new RiemanIntegrator(400).integrate(-5d, 5d, gausss), 1e-2);
    }
    
}
