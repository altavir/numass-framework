/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass;

import hep.dataforge.maths.integration.GaussRuleIntegrator;
import hep.dataforge.maths.integration.UnivariateIntegrator;

/**
 *
 * @author Alexander Nozik
 */
public class NumassIntegrator {

    private static UnivariateIntegrator fastInterator;
    private static UnivariateIntegrator defaultIntegrator;
    private static UnivariateIntegrator highDensityIntegrator;

    public static UnivariateIntegrator getFastInterator() {
        if (fastInterator == null) {
            fastInterator = new GaussRuleIntegrator(100);
        }
        return fastInterator;
    }

    public static UnivariateIntegrator getDefaultIntegrator() {
        if (defaultIntegrator == null) {
            defaultIntegrator = new GaussRuleIntegrator(300);
        }
        return defaultIntegrator;
    }

    public static UnivariateIntegrator getHighDensityIntegrator() {
        if (highDensityIntegrator == null) {
            highDensityIntegrator = new GaussRuleIntegrator(500);
        }
        return highDensityIntegrator;
    }

}
