package inr.numass.scripts

import hep.dataforge.context.Global
import hep.dataforge.grind.Grind
import hep.dataforge.meta.Meta
import hep.dataforge.stat.fit.FitManager
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.models.XYModel
import inr.numass.NumassPlugin

/**
 * Created by darksnake on 14-Dec-16.
 */


Locale.setDefault(Locale.US);
new NumassPlugin().startGlobal();


def fm = Global.instance().provide("fitting", FitManager.class).getFitManager();
def mm = fm.modelManager


Meta modelMeta = Grind.buildMeta(modelName: "sterile") {
    resolution(width: 8.3e-5, tailAlpha: 3e-3)
    transmission(trapping: "function::numass.trap.nominal") // numass.trap.nominal = 1.2e-4 - 4.5e-9 * Ei
}

/*

 'N'	= 2.76515e+06 Â± 2.4e+03	(0.00000,Infinity)
 'bkg'	= 41.195 Â± 0.053
 'E0'	= 18576.35 Â± 0.32
 'mnu2'	= 0.00 Â± 0.010
 'msterile2'	= 1000000.00 Â± 1.0
 'U2'	= 0.00314 Â± 0.0010
 'X'	= 0.12000 Â± 0.010	(0.00000,Infinity)
 'trap'	= 1.089 Â± 0.026
 */

Meta paramMeta = Grind.buildMeta("params") {
    N(value: 2.76515e+06, err: 30, lower: 0)
    bkg(value: 41.195, err: 0.1)
    E0(value: 18576.35, err: 0.1)
    mnu2(value: 0, err: 0.01)
    msterile2(value: 1000**2, err: 1)
    U2(value: 0.00314, err: 1e-3)
    X(value: 0.12000, err: 0.01, lower: 0)
    trap(value: 1.089, err: 0.05)
}

XYModel model = mm.buildModel(modelMeta)

ParamSet allPars = ParamSet.fromMeta(paramMeta);

def a = 16000;
def b = 18600;
def step = 50;


for (double x = a; x < b; x += step) {
    println "${x}\t${model.value(x, allPars)}"
}

Global.terminate()