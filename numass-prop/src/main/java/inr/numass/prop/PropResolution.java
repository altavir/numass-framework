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
package inr.numass.prop;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.maths.NamedDoubleSet;
import hep.dataforge.names.NamedUtils;
import inr.numass.models.Transmission;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;

/**
 *
 * @author Darksnake
 */
public class PropResolution extends AbstractTransmission {

    private static final String[] list = {"w", "dw", "base"};

    private final double cutoff = 6d;
    private final Transmission base;
    

    public PropResolution(Transmission base) {
        super(NamedUtils.combineNamesWithEquals(list, base.names().asArray()));
        this.base = base;
    }

    /**
     * @return the base
     */
    public Transmission getBase() {
        return base;
    }


//    private double getPos(NamedDoubleSet pars) {
//        return pars.getValue("pos");
//    }


    private double getW(NamedDoubleSet pars, double Ebeta) {
        return pars.getValue("w")*Math.sqrt(Ebeta);
    }

    private double getdW(NamedDoubleSet pars) {
        if (pars.names().contains("dw")) {
            return pars.getValue("dw");
        } else {
            return 0;
        }
    }

    private double getBase(NamedDoubleSet pars) {
        if (pars.names().contains("base")) {
            return pars.getValue("base");
        } else {
            return 0;
        }
    }

    @Override
    public boolean providesDeriv(String name) {
        return false;
    }

    /**
     *
     * @param Ebeta - начальная энергия электрона, полученная в результате
     * бета-распада
     * @param Ef - конечная энергия электрона, как она зарегистрирована прибором
     * @param pars - параметры
     * @return - вероятность получить такую конечную энергию при такой начальной
     */
    @Override
    public double getValue(NamedDoubleSet pars, double Ebeta, double Ef) {
        double res = 0;
        double baseValue = getBase().getValue(pars, Ebeta, Ef);
        double w = getW(pars, Ebeta);
        if (abs(Ef - Ebeta) <= cutoff * w) {
            double aux;
            if (Ef > Ebeta) {
                aux = (Ef - Ebeta) / (w*(1+getdW(pars)));
            } else {
                aux = (Ef - Ebeta) / (w*(1-getdW(pars)));
            }
            res = exp(-aux * aux / 2) / w / sqrt(2 * Math.PI);
        }

        return res * (1 - getBase(pars)) + getBase(pars)*baseValue;
    }

    @Override
    public double getDeriv(String name, NamedDoubleSet pars, double Ebeta, double Ef) {
        double w = getW(pars, Ebeta);
        if (abs(Ef - Ebeta) > cutoff * w) {
            return 0;
        }
        double res;
        switch (name) {
            case "pos":
                res = this.getValue(pars, Ebeta, Ef) * (Ef - Ebeta) / w / w * (1 - getBase(pars));
                break;
            case "w":
                res = this.getValue(pars, Ebeta, Ef) * ((Ef - Ebeta) * (Ef - Ebeta) / w / w / w - 1 / w) * (1 - getBase(pars));
                break;
            case "dw":
                throw new NotDefinedException();
        //            case "base":
        //                return 1 / getPos(pars) - this.value(E, pars) * (E - getPos(pars)) / getW(pars) / getW(pars);
            case "base":
                return getBase().getValue(pars, Ebeta, Ef) - getValue(pars, Ebeta, Ef);
            default:
                res = 0;
        }
        return res;
    }    

    @Override
    protected double[] getBorders(NamedDoubleSet pars, double out) {
        double[] res = new double[3];
        
        res[0] = 0;
        res[1] = Math.max(out - cutoff*getW(pars, out),0);
        res[2] = out + cutoff*getW(pars, out);
        return res;
    }

}
