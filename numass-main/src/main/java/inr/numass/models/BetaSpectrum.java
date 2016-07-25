/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.models;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.fitting.parametric.AbstractParametricFunction;
import hep.dataforge.values.NamedValueSet;
import hep.dataforge.values.ValueProvider;
import java.io.File;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;

/**
 *
 * @author Darksnake
 */
public class BetaSpectrum extends AbstractParametricFunction implements RangedNamedSetSpectrum {

    static final double K = 1E-23;
    // константа K в формуле, подбираем ее таким образом, чтобы нормировка соответствовала скрости счета
    static final String[] list = {"E0", "mnu2", "msterile2", "U2"};
    FSS fss = null;

    public BetaSpectrum() {
        super(list);
    }

    public BetaSpectrum(File FSSFile) {
        super(list);
        if (FSSFile != null) {
            this.fss = new FSS(FSSFile);
        }
    }

    double derivRoot(int n, double E0, double mnu2, double E) throws NotDefinedException {
        //ограничение

        double D = E0 - E;//E0-E
        double res;
        if (D == 0) {
            return 0;
        }

        if (mnu2 >= 0) {
            if (E >= (E0 - sqrt(mnu2))) {
                return 0;
            }
            double bare = sqrt(D * D - mnu2);
            switch (n) {
                case 0:
                    res = factor(E) * (2 * D * D - mnu2) / bare;
                    break;
                case 1:
                    res = -factor(E) * 0.5 * D / bare;
                    break;
                default:
                    return 0;
            }
        } else {
            double mu = sqrt(-0.66 * mnu2);
            if (E >= (E0 + mu)) {
                return 0;
            }
            double root = sqrt(Math.max(D * D - mnu2, 0));
            double exp = exp(-1 - D / mu);
            switch (n) {
                case 0:
                    res = factor(E) * (D * (D + mu * exp) / root + root * (1 - exp));
                    break;
                case 1:
                    res = factor(E) * (-(D + mu * exp) / root * 0.5 - root * exp * (1 + D / mu) / 3 / mu);
                    break;
                default:
                    return 0;
            }
        }

        return res;
    }

    double derivRootsterile(String name, double E, ValueProvider pars) throws NotDefinedException {
        double E0 = pars.getDouble("E0");
        double mnu2 = pars.getDouble("mnu2");
        double mst2 = pars.getDouble("msterile2");
        double u2 = pars.getDouble("U2");

        switch (name) {
            case "E0":
                if (u2 == 0) {
                    return derivRoot(0, E0, mnu2, E);
                }
                return u2 * derivRoot(0, E0, mst2, E) + (1 - u2) * derivRoot(0, E0, mnu2, E);
            case "mnu2":
                return (1 - u2) * derivRoot(1, E0, mnu2, E);
            case "msterile2":
                if (u2 == 0) {
                    return 0;
                }
                return u2 * derivRoot(1, E0, mst2, E);
            case "U2":
                return root(E0, mst2, E) - root(E0, mnu2, E);
            default:
                return 0;
        }

    }

    @Override
    public double derivValue(String name, double E, NamedValueSet pars) throws NotDefinedException {
        if (this.fss == null) {
            return this.derivRootsterile(name, E, pars);
        }
        double res = 0;
        int i;
        for (i = 0; i < fss.size(); i++) {
            res += fss.getP(i) * this.derivRootsterile(name, E + fss.getE(i), pars);
        }
        return res;
    }

    double factor(double E) {
        return K * pfactor(E);
    }

    @Override
    public Double max(NamedValueSet set) {
        return set.getDouble("E0");
    }

    @Override
    public Double min(NamedValueSet set) {
        return 0d;
    }

    double pfactor(double E) {
        double me = 0.511006E6;
        double Etot = E + me;
        double pe = sqrt(E * (E + 2d * me));
        double ve = pe / Etot;
        double yfactor = 2d * 2d * 1d / 137.039 * Math.PI;
        double y = yfactor / ve;
        double Fn = y / abs(1d - exp(-y));
        double Fermi = Fn * (1.002037 - 0.001427 * ve);
        double res = Fermi * pe * Etot;
        return res;
    }

    @Override
    public boolean providesDeriv(String name) {
        return true;
    }

    double root(double E0, double mnu2, double E) {
        /*чистый бета-спектр*/
        double D = E0 - E;//E0-E
        double res;
        double bare = factor(E) * D * sqrt(Math.max(D * D - mnu2, 0));
        if (D == 0) {
            return 0;
        }
        if (mnu2 >= 0) {
            res = Math.max(bare, 0);
        } else {
            if (D + 0.812 * sqrt(-mnu2) <= 0) {
                return 0;              //sqrt(0.66)
            }
            double aux = sqrt(-mnu2 * 0.66) / D;
            res = Math.max(bare * (1 + aux * exp(-1 - 1 / aux)), 0);
        }
        return res;
    }

    double rootsterile(double E, ValueProvider pars) {
        double E0 = pars.getDouble("E0");
        double mnu2 = pars.getDouble("mnu2");
        double mst2 = pars.getDouble("msterile2");
        double u2 = pars.getDouble("U2");

        if (u2 == 0) {
            return root(E0, mnu2, E);
        }
        return u2 * root(E0, mst2, E) + (1 - u2) * root(E0, mnu2, E);
        // P(rootsterile)+ (1-P)root
    }

    public void setFSS(File FSSFile) {
        if (FSSFile == null) {
            this.fss = null;
        } else {
            this.fss = new FSS(FSSFile);
        }
    }

    @Override
    public double value(double E, NamedValueSet pars) {
        if (this.fss == null) {
            return rootsterile(E, pars);
        }
        /*Учет спектра конечных состояний*/
        int i;
        double res = 0;
        for (i = 0; i < fss.size(); i++) {
            res += fss.getP(i) * this.rootsterile(E + fss.getE(i), pars);
        }
        return res;
//        return rootsterile(E, pars);

    }

    @Override
    protected double getDefaultParameter(String name) {
        switch (name) {
            case "mnu2":
                return 0;
            case "U2":
                return 0;
            case "msterile2":
                return 0;
            default:
                return super.getDefaultParameter(name);
        }
    }
}
