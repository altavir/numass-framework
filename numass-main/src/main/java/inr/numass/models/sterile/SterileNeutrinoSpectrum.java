/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile;

import hep.dataforge.context.Context;
import hep.dataforge.context.GlobalContext;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.fitting.parametric.AbstractParametricFunction;
import hep.dataforge.fitting.parametric.ParametricBiFunction;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.NamedValueSet;
import inr.numass.NumassIntegrator;
import inr.numass.NumassContext;
import inr.numass.models.FSS;
import java.util.stream.DoubleStream;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.EnumeratedRealDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.SynchronizedRandomGenerator;

/**
 * Compact all-in-one model for sterile neutrino spectrum
 *
 * @author Alexander Nozik
 */
@NodeDef(name = "resolution")
@NodeDef(name = "transmission")
@ValueDef(name = "fssFile")

public class SterileNeutrinoSpectrum extends AbstractParametricFunction {

    private static final String[] list = {"X", "trap", "E0", "mnu2", "msterile2", "U2"};

    private final RandomGenerator rnd;
    private RealDistribution fssDistribution;
    private FSS fss;

    /**
     * variables:Eo offset,Ein; parameters: "mnu2", "msterile2", "U2"
     */
    private final ParametricBiFunction source = new NumassBeta();

    /**
     * variables:Ein,Eout; parameters: "A"
     */
    private final ParametricBiFunction transmission;
    /**
     * variables:Eout,U; parameters: "X", "trap"
     */
    private final ParametricBiFunction resolution;
    
    private boolean useMC;

    public SterileNeutrinoSpectrum(Context context, Meta configuration) {
        super(list);
        rnd = new SynchronizedRandomGenerator(new JDKRandomGenerator());
        if (configuration.hasValue("fssFile")) {
            fss = new FSS(context.io().getFile(configuration.getString("fssFile")));
            fssDistribution = new EnumeratedRealDistribution(rnd, fss.getEs(), fss.getPs());
        }

        transmission = new NumassTransmission(configuration.getNodeOrEmpty("transmission"));
        resolution = new NumassResolution(configuration.getNode("resolution", Meta.empty()));
        this.useMC = configuration.getBoolean("useMC",false);
    }
    
    public SterileNeutrinoSpectrum(Meta configuration) {
        this(GlobalContext.instance(),configuration);
    }
    
    public SterileNeutrinoSpectrum() {
        this(GlobalContext.instance(), Meta.empty());
    }

    @Override
    public double derivValue(String parName, double u, NamedValueSet set) {
        switch (parName) {
            case "U2":
            case "msterile2":
            case "mnu2":
            case "E0":
                return integrate(u, source.derivative(parName), transmission, resolution, set);
            case "X":
            case "trap":
                return integrate(u, source, transmission, resolution.derivative(parName), set);
            default:
                throw new NotDefinedException();
        }
    }

    @Override
    public double value(double u, NamedValueSet set) {
        if (useDirect()) {
            return integrateDirect(u, source, transmission, resolution, set);
        } else {
            return integrate(u, source, transmission, resolution, set);
        }
    }

    private int numCalls(double u) {
        return 100000;
    }

    private boolean useDirect() {
        return !useMC;
    }

    @Override
    public boolean providesDeriv(String name) {
        return source.providesDeriv(name) && transmission.providesDeriv(name) && resolution.providesDeriv(name);
    }

    /**
     * Random E generator
     * @param a
     * @param b
     * @return 
     */
    private double rndE(double a, double b) {
        return rnd.nextDouble() * (b - a) + a;
    }

    /**
     * Monte-Carlo integration of spectrum
     *
     * @param u
     * @param sourceFunction
     * @param transmissionFunction
     * @param resolutionFunction
     * @param set
     * @return
     */
    private double integrate(
            double u,
            ParametricBiFunction sourceFunction,
            ParametricBiFunction transmissionFunction,
            ParametricBiFunction resolutionFunction,
            NamedValueSet set) {

        int num = numCalls(u);
        double eMax = set.getDouble("E0") + 5d;
        if (u > eMax) {
            return 0;
        }

        double sum = DoubleStream.generate(() -> {
            // generate final state
            double fs;
            if (fssDistribution != null) {
                fs = fssDistribution.sample();
            } else {
                fs = 0;
            }

            double eIn = rndE(u, eMax);

            double eOut = rndE(u, eIn);

            double res = sourceFunction.value(fs, eIn, set)
                    * transmissionFunction.value(eIn, eOut, set)
                    * resolutionFunction.value(eOut, u, set);

            if (Double.isNaN(res)) {
                throw new Error();
            }
            return res;
        }).parallel().limit(num).sum();
        //triangle surface
        return Math.pow(eMax - u, 2d) / 2d * sum / num;
    }

    /**
     * Direct Gauss-Legandre integration
     * @param u
     * @param sourceFunction
     * @param transmissionFunction
     * @param resolutionFunction
     * @param set
     * @return 
     */
    private double integrateDirect(
            double u,
            ParametricBiFunction sourceFunction,
            ParametricBiFunction transmissionFunction,
            ParametricBiFunction resolutionFunction,
            NamedValueSet set) {

        double eMax = set.getDouble("E0") + 5d;

        if (u > eMax) {
            return 0;
        }

        UnivariateFunction fsSource;
        if (fss != null) {
            fsSource = (eIn) -> {
                double res = 0;
                for (int i = 0; i < fss.size(); i++) {
                    res += fss.getP(i) * sourceFunction.value(fss.getE(i), eIn, set);
                }
                return res;
            };
        } else {
            fsSource = (eIn) -> sourceFunction.value(0, eIn, set);
        }

        BivariateFunction transRes = (eIn, usp) -> {
            UnivariateFunction integrand = (eOut) -> transmissionFunction.value(eIn, eOut, set) * resolutionFunction.value(eOut, usp, set);
            return NumassIntegrator.getDefaultIntegrator().integrate(integrand, usp, eIn);
        };

        UnivariateFunction integrand = (eIn) -> {
            double p0 = NumassTransmission.p0(eIn, set);
            return fsSource.value(eIn) * (p0 * resolutionFunction.value(eIn, u, set) + transRes.value(eIn, u));
        };

        return NumassIntegrator.getDefaultIntegrator().integrate(integrand, u, eMax);
    }

}
