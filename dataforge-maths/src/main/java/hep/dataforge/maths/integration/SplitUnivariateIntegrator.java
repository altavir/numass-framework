/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.maths.integration;

import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Integrator that breaks an interval into subintervals and integrates each
 * interval with its own integrator;
 *
 * @author Alexander Nozik
 */
public class SplitUnivariateIntegrator extends UnivariateIntegrator<UnivariateIntegrand> {
    
    Map<Double, Supplier<UnivariateIntegrator>> segments;
    Supplier<UnivariateIntegrator> defaultIntegrator;

    @Override
    protected UnivariateIntegrand init(Double lower, Double upper, UnivariateFunction function) {
        return new UnivariateIntegrand(lower, upper, function);
    }

//    @Override
//    public UnivariateIntegrand evaluate(UnivariateIntegrand integrand, Predicate<UnivariateIntegrand> condition) {
//        Map<UnivariateIntegrand, Supplier<UnivariateIntegrator>> integrandMap;
//        
//        Map<UnivariateIntegrand, Double> res = new HashMap<>();
//        return new UnivariateIntegrand(integrand, 
//                res.keySet().stream().mapToDouble(it->it.getAbsoluteAccuracy()).sum(),
//                0,
//                res.keySet().stream().mapToInt(it->it.getIterations()).sum(),
//                0,
//                res.values().stream().mapToDouble(it->it).sum());
//    }

    @Override
    public UnivariateIntegrand evaluate(UnivariateIntegrand integrand, Predicate<UnivariateIntegrand> condition) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Predicate<UnivariateIntegrand> getDefaultStoppingCondition() {
        return (t) -> true;
    }

}
