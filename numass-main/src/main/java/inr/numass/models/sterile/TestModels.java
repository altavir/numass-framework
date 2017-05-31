/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile;

import hep.dataforge.context.Context;
import hep.dataforge.maths.MathPlugin;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.stat.parametric.ParametricFunction;
import inr.numass.Numass;
import inr.numass.models.*;
import org.apache.commons.math3.analysis.BivariateFunction;

/**
 *
 * @author Alexander Nozik
 */
public class TestModels {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Context context = Numass.buildContext();
        /*
        		<model modelName="sterile" fssFile="FS.txt">
			<resolution width = "1.22e-4" tailAlpha="1e-2"/>
			<transmission trapping="numass.trap2016"/>
		</model>
         */
        Meta meta = new MetaBuilder("model")
                .putNode(new MetaBuilder("resolution")
                        .putValue("width", 1.22e-4)
                        .putValue("tailAlpha", 1e-2)
                )
                .putNode(new MetaBuilder("transmission")
                        .putValue("trapping", "numass.trap2016")
                );
        ParametricFunction oldFunc = oldModel(context, meta);
        ParametricFunction newFunc = newModel(context, meta);

        ParamSet allPars = new ParamSet()
                .setPar("N", 7e+05, 1.8e+03, 0d, Double.POSITIVE_INFINITY)
                .setPar("bkg", 1d, 0.050)
                .setPar("E0", 18575d, 1.4)
                .setPar("mnu2", 0d, 1d)
                .setPar("msterile2", 1000d * 1000d, 0)
                .setPar("U2", 0.0, 1e-4, -1d, 1d)
                .setPar("X", 0.04, 0.01, 0d, Double.POSITIVE_INFINITY)
                .setPar("trap", 1, 0.01, 0d, Double.POSITIVE_INFINITY);

        for (double u = 14000; u < 18600; u += 100) {
//            double oldVal = oldFunc.value(u, allPars);
//            double newVal = newFunc.value(u, allPars);
            double oldVal = oldFunc.derivValue("trap", u, allPars);
            double newVal = newFunc.derivValue("trap", u, allPars);
            System.out.printf("%f\t%g\t%g\t%g%n", u, oldVal, newVal, 1d - oldVal / newVal);
        }
    }

    @SuppressWarnings("deprecation")
    private static ParametricFunction oldModel(Context context, Meta meta) {
        double A = meta.getDouble("resolution", meta.getDouble("resolution.width", 8.3e-5));//8.3e-5
        double from = meta.getDouble("from", 13900d);
        double to = meta.getDouble("to", 18700d);
        context.getChronicle().report("Setting up tritium model with real transmission function");
        BivariateFunction resolutionTail;
        if (meta.hasValue("resolution.tailAlpha")) {
            resolutionTail = ResolutionFunction.getAngledTail(meta.getDouble("resolution.tailAlpha"), meta.getDouble("resolution.tailBeta", 0));
        } else {
            resolutionTail = ResolutionFunction.getRealTail();
        }
        //RangedNamedSetSpectrum beta = new BetaSpectrum(context.io().getFile("FS.txt"));
        RangedNamedSetSpectrum beta = new BetaSpectrum();
        ModularSpectrum sp = new ModularSpectrum(beta, new ResolutionFunction(A, resolutionTail), from, to);
        if (meta.getBoolean("caching", false)) {
            context.getChronicle().report("Caching turned on");
            sp.setCaching(true);
        }
        //Adding trapping energy dependence

        if (meta.hasValue("transmission.trapping")) {
            BivariateFunction trap = MathPlugin.buildFrom(context).buildBivariateFunction(meta.getString("transmission.trapping"));
            sp.setTrappingFunction(trap);
        }

        return new NBkgSpectrum(sp);
    }

    private static ParametricFunction newModel(Context context, Meta meta) {
        SterileNeutrinoSpectrum sp = new SterileNeutrinoSpectrum(context, meta);
        return new NBkgSpectrum(sp);
    }

}
