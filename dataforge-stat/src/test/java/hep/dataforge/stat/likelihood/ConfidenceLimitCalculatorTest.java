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
package hep.dataforge.stat.likelihood;

import hep.dataforge.stat.fit.IntervalEstimate;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.junit.*;

import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Alexander Nozik
 */
public class ConfidenceLimitCalculatorTest {
    
    private double sigma = 2d;
    private double a = -3d * sigma;
    private double b = 3d * sigma;
    private UnivariateFunction gaussian = new Gaussian(0d, sigma);
  
    /**
     *
     */
    public ConfidenceLimitCalculatorTest() {

    }

    /**
     *
     */
    @BeforeClass
    public static void setUpClass() {
    }

    /**
     *
     */
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     *
     */
    @Before
    public void setUp() {
    }

    /**
     *
     */
    @After
    public void tearDown() {
    }

    /**
     * Test of get95CLUpperLimit method, of class ConfidenceLimitCalculator.
     */
    @Test
    public void testGet95CLUpperLimit() {
        System.out.println("get95CLUpperLimit");
        ConfidenceLimitCalculator instance = new ConfidenceLimitCalculator(gaussian, a, b, 30);
        double expResult = 1.64 * sigma;
        double result = instance.get95CLUpperLimit().getSecond();
        assertEquals(expResult, result, 0.1);

    }

    /**
     *
     */
    @Test
    public void testGetLimis() {
        ConfidenceLimitCalculator instance = new ConfidenceLimitCalculator(gaussian, a, b, 30);
        IntervalEstimate limit = instance.getEstimate("pos");
        limit.print(new PrintWriter(System.out,true));
    }
}