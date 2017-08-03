/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.maths.integration.GaussRuleIntegrator
import hep.dataforge.maths.integration.UnivariateIntegrator
import inr.numass.models.LossCalculator
import org.apache.commons.math3.analysis.UnivariateFunction

UnivariateIntegrator integrator = new GaussRuleIntegrator(400);

def exPos = 12.587;
def ionPos = 11.11;
def exW = 1.20;
def ionW = 11.02;
def exIonRatio = 2.43;

def cutoff = 20d

UnivariateFunction loss = LossCalculator.getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio);


println integrator.integrate(loss,0,600);
println integrator.integrate(loss,0, cutoff);
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