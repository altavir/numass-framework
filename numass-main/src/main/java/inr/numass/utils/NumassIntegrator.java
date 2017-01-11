/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import hep.dataforge.maths.integration.GaussRuleIntegrator;
import hep.dataforge.maths.integration.UnivariateIntegrator;

/**
 *
 * @author Alexander Nozik
 */
public class NumassIntegrator {

    private static double mult = 1.0;

    private static UnivariateIntegrator fastInterator;
    private static UnivariateIntegrator defaultIntegrator;
    private static UnivariateIntegrator highDensityIntegrator;

    public static UnivariateIntegrator getFastInterator() {
        if (fastInterator == null) {
            fastInterator = new GaussRuleIntegrator((int) (mult*100));
        }
        return fastInterator;
    }

    public static UnivariateIntegrator getDefaultIntegrator() {
        if (defaultIntegrator == null) {
            defaultIntegrator = new GaussRuleIntegrator((int) (mult*300));
        }
        return defaultIntegrator;
    }

    public static UnivariateIntegrator getHighDensityIntegrator() {
        if (highDensityIntegrator == null) {
            highDensityIntegrator = new GaussRuleIntegrator((int) (mult*500));
        }
        return highDensityIntegrator;
    }

}
