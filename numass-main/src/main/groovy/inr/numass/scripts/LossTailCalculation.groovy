/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.maths.integration.GaussRuleIntegrator;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import inr.numass.models.LossCalculator;
import org.apache.commons.math3.analysis.UnivariateFunction

UnivariateIntegrator integrator = new GaussRuleIntegrator(400);
def exPos = 12.878;
def ionPos = 13.86;
def exW = 1.32;
def ionW = 12.47;
def exIonRatio = 3.96;

def cutoff = 25d

UnivariateFunction loss = LossCalculator.getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio);


println integrator.integrate(loss,0,600);
println integrator.integrate(loss,0,cutoff);
println integrator.integrate(loss,cutoff,600d);

println (integrator.integrate(loss,0,cutoff) + integrator.integrate(loss,cutoff,3000d));
//double tailValue = (Math.atan((ionPos-cutoff)*2d/ionW) + 0.5*Math.PI)*ionW/2;
//println tailValue
//println integrator.integrate(loss,0,100);
//println integrator.integrate(loss,100,600);

//def lorentz = {eps->
//    double z = 4 * (eps - ionPos) * (eps - ionPos);
//    1 / (1 + z / ionW / ionW);
//}
//
//println(integrator.integrate(lorentz, cutoff, 800))