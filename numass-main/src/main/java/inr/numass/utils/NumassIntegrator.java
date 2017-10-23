/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import hep.dataforge.maths.integration.GaussRuleIntegrator;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import org.slf4j.LoggerFactory;

/**
 * @author Alexander Nozik
 */
public class NumassIntegrator {

    private static double mult = 1.0;//for testing purposes

    private static UnivariateIntegrator fastInterator;
    private static UnivariateIntegrator defaultIntegrator;
    private static UnivariateIntegrator highDensityIntegrator;

    public synchronized static UnivariateIntegrator getFastInterator() {
        if (fastInterator == null) {
            LoggerFactory.getLogger(NumassIntegrator.class).debug("Creating fast integrator");
            fastInterator = new GaussRuleIntegrator((int) (mult * 100));
        }
        return fastInterator;
    }

    public synchronized static UnivariateIntegrator getDefaultIntegrator() {
        if (defaultIntegrator == null) {
            LoggerFactory.getLogger(NumassIntegrator.class).debug("Creating default integrator");
            defaultIntegrator = new GaussRuleIntegrator((int) (mult * 300));
        }
        return defaultIntegrator;
    }

    public synchronized static UnivariateIntegrator getHighDensityIntegrator() {
        if (highDensityIntegrator == null) {
            LoggerFactory.getLogger(NumassIntegrator.class).debug("Creating high precision integrator");
            highDensityIntegrator = new GaussRuleIntegrator((int) (mult * 500));
        }
        return highDensityIntegrator;
    }

}
