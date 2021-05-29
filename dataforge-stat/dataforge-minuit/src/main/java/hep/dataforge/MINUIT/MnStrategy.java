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
 * API class for defining three levels of strategies: low (0), medium (1), high
 * (&ge;2).
 * <p>
 * At many places in the analysis of the FCN (the user provided function),
 * MINUIT must decide whether to be <I>safe</I> and waste a few function calls
 * in order to know where it is, or to be <I>fast</I> and attempt to get the
 * requested results with the fewest possible calls at a certain risk of not
 * obtaining the precision desired by the user. In order to allow the user to
 * infuence these decisions, the MnStrategy class allows the user to control
 * different settings. MnStrategy can be instantiated with three different
 * minimization quality levels for low (0), medium (1) and high (2) quality.
 * Default settings for iteration cycles and tolerances are initialized then.
 * <p>
 * The default setting is set for medium quality. Value 0 (low) indicates to
 * MINUIT that it should economize function calls; it is intended for cases
 * where there are many variable parameters and/or the function takes a long
 * time to calculate and/or the user is not interested in very precise values
 * for parameter errors. On the other hand, value 2 (high) indicates that MINUIT
 * is allowed to waste function calls in order to be sure that all values are
 * precise; it is it is intended for cases where the function is evaluated in a
 * relatively short time and/or where the parameter errors must be calculated
 * reliably.
 * <p>In addition all constants set in MnStrategy can be changed individually by
 * the user, e.g. the number of iteration cycles in the numerical gradient.
 * <p>
 * <p>
 * Acts on: Migrad (behavioural), Minos (lowers strategy by 1 for Minos-own
 * minimization), Hesse (iterations), Numerical2PDerivative (iterations)
 *
 * @author Darksnake
 * @version $Id$
 */
public class MnStrategy {
    private int theGradNCyc;
    private double theGradTlr;
    private double theGradTlrStp;
    private int theHessGradNCyc;
    //default strategy
    private int theHessNCyc;
    private double theHessTlrG2;
    private double theHessTlrStp;
    private int theStrategy;

    /**
     * Creates a MnStrategy object with the default strategy (medium)
     */
    public MnStrategy() {
        setMediumStrategy();
    }

    //user defined strategy (0, 1, >=2)

    /**
     * Creates a MnStrategy object with the user specified strategy.
     *
     * @param stra The use defined strategy, 0=low, 1 medium, 2=high.
     */
    public MnStrategy(int stra) {
        if (stra == 0) {
            setLowStrategy();
        } else if (stra == 1) {
            setMediumStrategy();
        } else {
            setHighStrategy();
        }
    }

    /**
     * <p>gradientNCycles.</p>
     *
     * @return a int.
     */
    public int gradientNCycles() {
        return theGradNCyc;
    }

    /**
     * <p>gradientStepTolerance.</p>
     *
     * @return a double.
     */
    public double gradientStepTolerance() {
        return theGradTlrStp;
    }

    /**
     * <p>gradientTolerance.</p>
     *
     * @return a double.
     */
    public double gradientTolerance() {
        return theGradTlr;
    }

    /**
     * <p>hessianG2Tolerance.</p>
     *
     * @return a double.
     */
    public double hessianG2Tolerance() {
        return theHessTlrG2;
    }

    /**
     * <p>hessianGradientNCycles.</p>
     *
     * @return a int.
     */
    public int hessianGradientNCycles() {
        return theHessGradNCyc;
    }

    /**
     * <p>hessianNCycles.</p>
     *
     * @return a int.
     */
    public int hessianNCycles() {
        return theHessNCyc;
    }

    /**
     * <p>hessianStepTolerance.</p>
     *
     * @return a double.
     */
    public double hessianStepTolerance() {
        return theHessTlrStp;
    }

    /**
     * <p>isHigh.</p>
     *
     * @return a boolean.
     */
    public boolean isHigh() {
        return theStrategy >= 2;
    }

    /**
     * <p>isLow.</p>
     *
     * @return a boolean.
     */
    public boolean isLow() {
        return theStrategy <= 0;
    }

    /**
     * <p>isMedium.</p>
     *
     * @return a boolean.
     */
    public boolean isMedium() {
        return theStrategy == 1;
    }

    /**
     * <p>setGradientNCycles.</p>
     *
     * @param n a int.
     */
    public void setGradientNCycles(int n) {
        theGradNCyc = n;
    }

    /**
     * <p>setGradientStepTolerance.</p>
     *
     * @param stp a double.
     */
    public void setGradientStepTolerance(double stp) {
        theGradTlrStp = stp;
    }

    /**
     * <p>setGradientTolerance.</p>
     *
     * @param toler a double.
     */
    public void setGradientTolerance(double toler) {
        theGradTlr = toler;
    }

    /**
     * <p>setHessianG2Tolerance.</p>
     *
     * @param toler a double.
     */
    public void setHessianG2Tolerance(double toler) {
        theHessTlrG2 = toler;
    }

    /**
     * <p>setHessianGradientNCycles.</p>
     *
     * @param n a int.
     */
    public void setHessianGradientNCycles(int n) {
        theHessGradNCyc = n;
    }

    /**
     * <p>setHessianNCycles.</p>
     *
     * @param n a int.
     */
    public void setHessianNCycles(int n) {
        theHessNCyc = n;
    }

    /**
     * <p>setHessianStepTolerance.</p>
     *
     * @param stp a double.
     */
    public void setHessianStepTolerance(double stp) {
        theHessTlrStp = stp;
    }

    void setHighStrategy() {
        theStrategy = 2;
        setGradientNCycles(5);
        setGradientStepTolerance(0.1);
        setGradientTolerance(0.02);
        setHessianNCycles(7);
        setHessianStepTolerance(0.1);
        setHessianG2Tolerance(0.02);
        setHessianGradientNCycles(6);
    }

    /**
     * <p>setLowStrategy.</p>
     */
    public void setLowStrategy() {
        theStrategy = 0;
        setGradientNCycles(2);
        setGradientStepTolerance(0.5);
        setGradientTolerance(0.1);
        setHessianNCycles(3);
        setHessianStepTolerance(0.5);
        setHessianG2Tolerance(0.1);
        setHessianGradientNCycles(1);
    }

    /**
     * <p>setMediumStrategy.</p>
     */
    public void setMediumStrategy() {
        theStrategy = 1;
        setGradientNCycles(3);
        setGradientStepTolerance(0.3);
        setGradientTolerance(0.05);
        setHessianNCycles(5);
        setHessianStepTolerance(0.3);
        setHessianG2Tolerance(0.05);
        setHessianGradientNCycles(2);
    }

    /**
     * <p>strategy.</p>
     *
     * @return a int.
     */
    public int strategy() {
        return theStrategy;
    }
}
