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

import hep.dataforge.maths.NamedMatrix;
import hep.dataforge.maths.NamedVector;
import hep.dataforge.stat.parametric.ParametricValue;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.*;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Nozik
 */
public class MarginalFunctionBuilderTest {

    static final String[] nameList = {"par1", "par2", "par3"};
    RandomGenerator generator = new JDKRandomGenerator(54321);
    NamedVector zero;
    NamedMatrix cov;

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

    MarginalFunctionBuilder instance;
    ParametricValue testFunc;

    /**
     *
     */
    public MarginalFunctionBuilderTest() {
    }

    /**
     *
     */
    @Before
    public void setUp() {
        double[] d = {1d, 4d, 0.25d};
        RealMatrix mat = new DiagonalMatrix(d);
        cov = new NamedMatrix(nameList, mat);
        testFunc = new Gaussian(cov);
        ArrayRealVector vector = new ArrayRealVector(cov.getNames().size());
        zero = new NamedVector(nameList, vector);
        instance = new MarginalFunctionBuilder()
                .setParameters(zero)
                .setFunction(testFunc);
    }

    /**
     *
     */
    @After
    public void tearDown() {
    }

    /**
     * Test of getMarginalValue method, of class Marginalizer.
     */
    @Test
    public void testGetMarginalValue() {
        System.out.println("getMarginalValue");
        int maxCalls = 1000;
        double expResult = 1d / sqrt(2 * Math.PI);
        double result = instance.setMaxCalls(maxCalls)
                .setNormalSampler(generator, zero, cov, "par2", "par3")
                .build().value(zero);
        assertEquals(expResult, result, 0.01);
        System.out.printf("The expected value is %g, the test result is %g%n", expResult, result);
        System.out.printf("On %d calls the relative discrepancy is %g%n", maxCalls, abs(result - expResult) / result);
    }

    /**
     * Test of getNorm method, of class Marginalizer.
     */
    @Test
    public void testGetNorm() {
        System.out.println("getNorm");
        int maxCalls = 10000;
        double expResult = 1.0;
        //zero here is redundant
        double result = instance.setMaxCalls(maxCalls)
                .setNormalSampler(generator, zero, cov, nameList)
                .build().value(zero);
        assertEquals(expResult, result, 0.05);
        System.out.printf("The expected value is %g, the test result is %g%n", expResult, result);
        System.out.printf("On %d calls the relative discrepancy is %g%n", maxCalls, abs(result - expResult) / result);
    }
}