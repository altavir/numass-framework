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
def exPos = 12.695;
def ionPos = 13.29;
def exW = 1.22;
def ionW = 11.99;
def exIonRatio = 3.6;

def cutoff = 25d

UnivariateFunction func = {double eps -> 
    if (eps <= 0) {
        return 0;
    }
    double z1 = eps - exPos;
    double ex = exIonRatio * Math.exp(-2 * z1 * z1 / exW / exW);

    double z = 4 * (eps - ionPos) * (eps - ionPos);
    double ion = 1 / (1 + z / ionW / ionW);

    double res;
    if (eps < exPos) {
        res = ex;
    } else {
        res = Math.max(ex, ion);
    }

    return res;
};

//caclulating lorentz integral analythically
double tailNorm = (Math.atan((ionPos - cutoff) * 2d / ionW) + 0.5 * Math.PI) * ionW / 2d;
final double norm = integrator.integrate(func, 0d, cutoff) + tailNorm;

println 1/norm;