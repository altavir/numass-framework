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
package hep.dataforge.MINUIT;

import hep.dataforge.maths.functions.MultiFunction;
import hep.dataforge.stat.fit.MINUITPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * API class for Contours error analysis (2-dim errors). Minimization has to be
 * done before and minimum must be valid. Possibility to ask only for the points
 * or the points and associated Minos errors.
 *
 * @version $Id$
 * @author Darksnake
 */
public class MnContours {
    private MultiFunction theFCN;
    private FunctionMinimum theMinimum;
    private MnStrategy theStrategy;

    /**
     * construct from FCN + minimum
     *
     * @param fcn a {@link MultiFunction} object.
     * @param min a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     */
    public MnContours(MultiFunction fcn, FunctionMinimum min) {
        this(fcn, min, MnApplication.DEFAULT_STRATEGY);
    }

    /**
     * construct from FCN + minimum + strategy
     *
     * @param stra a int.
     * @param min a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnContours(MultiFunction fcn, FunctionMinimum min, int stra) {
        this(fcn, min, new MnStrategy(stra));
    }

    /**
     * construct from FCN + minimum + strategy
     *
     * @param stra a {@link hep.dataforge.MINUIT.MnStrategy} object.
     * @param min a {@link hep.dataforge.MINUIT.FunctionMinimum} object.
     * @param fcn a {@link MultiFunction} object.
     */
    public MnContours(MultiFunction fcn, FunctionMinimum min, MnStrategy stra) {
        theFCN = fcn;
        theMinimum = min;
        theStrategy = stra;
    }

    /**
     * <p>contour.</p>
     *
     * @param px a int.
     * @param py a int.
     * @return a {@link hep.dataforge.MINUIT.ContoursError} object.
     */
    public ContoursError contour(int px, int py) {
        return contour(px, py, 1);
    }

    /**
     * <p>contour.</p>
     *
     * @param px a int.
     * @param py a int.
     * @param errDef a double.
     * @return a {@link hep.dataforge.MINUIT.ContoursError} object.
     */
    public ContoursError contour(int px, int py, double errDef) {
        return contour(px, py, errDef, 20);
    }

    /**
     * Causes a CONTOURS error analysis and returns the result in form of
     * ContoursError. As a by-product ContoursError keeps the MinosError
     * information of parameters parx and pary. The result ContoursError can be
     * easily printed using MnPrint or toString().
     *
     * @param npoints a int.
     * @param px a int.
     * @param py a int.
     * @param errDef a double.
     * @return a {@link hep.dataforge.MINUIT.ContoursError} object.
     */
    public ContoursError contour(int px, int py, double errDef, int npoints) {
        errDef *= theMinimum.errorDef();
        assert (npoints > 3);
        int maxcalls = 100 * (npoints + 5) * (theMinimum.userState().variableParameters() + 1);
        int nfcn = 0;

        List<Range> result = new ArrayList<>(npoints);
        List<MnUserParameterState> states = new ArrayList<>();
        double toler = 0.05;

        //get first four points
        MnMinos minos = new MnMinos(theFCN, theMinimum, theStrategy);

        double valx = theMinimum.userState().value(px);
        double valy = theMinimum.userState().value(py);

        MinosError mex = minos.minos(px, errDef);
        nfcn += mex.nfcn();
        if (!mex.isValid()) {
            MINUITPlugin.logStatic("MnContours is unable to find first two points.");
            return new ContoursError(px, py, result, mex, mex, nfcn);
        }
        Range ex = mex.range();

        MinosError mey = minos.minos(py, errDef);
        nfcn += mey.nfcn();
        if (!mey.isValid()) {
            MINUITPlugin.logStatic("MnContours is unable to find second two points.");
            return new ContoursError(px, py, result, mex, mey, nfcn);
        }
        Range ey = mey.range();

        MnMigrad migrad = new MnMigrad(theFCN, theMinimum.userState().copy(), new MnStrategy(Math.max(0, theStrategy.strategy() - 1)));

        migrad.fix(px);
        migrad.setValue(px, valx + ex.getSecond());
        FunctionMinimum exy_up = migrad.minimize();
        nfcn += exy_up.nfcn();
        if (!exy_up.isValid()) {
            MINUITPlugin.logStatic("MnContours is unable to find upper y value for x parameter " + px + ".");
            return new ContoursError(px, py, result, mex, mey, nfcn);
        }

        migrad.setValue(px, valx + ex.getFirst());
        FunctionMinimum exy_lo = migrad.minimize();
        nfcn += exy_lo.nfcn();
        if (!exy_lo.isValid()) {
            MINUITPlugin.logStatic("MnContours is unable to find lower y value for x parameter " + px + ".");
            return new ContoursError(px, py, result, mex, mey, nfcn);
        }

        MnMigrad migrad1 = new MnMigrad(theFCN, theMinimum.userState().copy(), new MnStrategy(Math.max(0, theStrategy.strategy() - 1)));
        migrad1.fix(py);
        migrad1.setValue(py, valy + ey.getSecond());
        FunctionMinimum eyx_up = migrad1.minimize();
        nfcn += eyx_up.nfcn();
        if (!eyx_up.isValid()) {
            MINUITPlugin.logStatic("MnContours is unable to find upper x value for y parameter " + py + ".");
            return new ContoursError(px, py, result, mex, mey, nfcn);
        }

        migrad1.setValue(py, valy + ey.getFirst());
        FunctionMinimum eyx_lo = migrad1.minimize();
        nfcn += eyx_lo.nfcn();
        if (!eyx_lo.isValid()) {
            MINUITPlugin.logStatic("MnContours is unable to find lower x value for y parameter " + py + ".");
            return new ContoursError(px, py, result, mex, mey, nfcn);
        }

        double scalx = 1. / (ex.getSecond() - ex.getFirst());
        double scaly = 1. / (ey.getSecond() - ey.getFirst());

        result.add(new Range(valx + ex.getFirst(), exy_lo.userState().value(py)));
        result.add(new Range(eyx_lo.userState().value(px), valy + ey.getFirst()));
        result.add(new Range(valx + ex.getSecond(), exy_up.userState().value(py)));
        result.add(new Range(eyx_up.userState().value(px), valy + ey.getSecond()));

        MnUserParameterState upar = theMinimum.userState().copy();
        upar.fix(px);
        upar.fix(py);

        int[] par = {px, py};
        MnFunctionCross cross = new MnFunctionCross(theFCN, upar, theMinimum.fval(), theStrategy, errDef);

        for (int i = 4; i < npoints; i++) {
            Range idist1 = result.get(result.size() - 1);
            Range idist2 = result.get(0);
            int pos2 = 0;
            double distx = idist1.getFirst() - idist2.getFirst();
            double disty = idist1.getSecond() - idist2.getSecond();
            double bigdis = scalx * scalx * distx * distx + scaly * scaly * disty * disty;

            for (int j = 0; j < result.size() - 1; j++) {
                Range ipair = result.get(j);
                double distx2 = ipair.getFirst() - result.get(j + 1).getFirst();
                double disty2 = ipair.getSecond() - result.get(j + 1).getSecond();
                double dist = scalx * scalx * distx2 * distx2 + scaly * scaly * disty2 * disty2;
                if (dist > bigdis) {
                    bigdis = dist;
                    idist1 = ipair;
                    idist2 = result.get(j + 1);
                    pos2 = j + 1;
                }
            }

            double a1 = 0.5;
            double a2 = 0.5;
            double sca = 1.;

            for (;;) {
                if (nfcn > maxcalls) {
                    MINUITPlugin.logStatic("MnContours: maximum number of function calls exhausted.");
                    return new ContoursError(px, py, result, mex, mey, nfcn);
                }

                double xmidcr = a1 * idist1.getFirst() + a2 * idist2.getFirst();
                double ymidcr = a1 * idist1.getSecond() + a2 * idist2.getSecond();
                double xdir = idist2.getSecond() - idist1.getSecond();
                double ydir = idist1.getFirst() - idist2.getFirst();
                double scalfac = sca * Math.max(Math.abs(xdir * scalx), Math.abs(ydir * scaly));
                double xdircr = xdir / scalfac;
                double ydircr = ydir / scalfac;
                double[] pmid = {xmidcr, ymidcr};
                double[] pdir = {xdircr, ydircr};

                MnCross opt = cross.cross(par, pmid, pdir, toler, maxcalls);
                nfcn += opt.nfcn();
                if (opt.isValid()) {
                    double aopt = opt.value();
                    if (pos2 == 0) {
                        result.add(new Range(xmidcr + (aopt) * xdircr, ymidcr + (aopt) * ydircr));
                    } else {
                        result.add(pos2, new Range(xmidcr + (aopt) * xdircr, ymidcr + (aopt) * ydircr));
                    }
                    break;
                }
                if (sca < 0.) {
                    MINUITPlugin.logStatic("MnContours is unable to find point " + (i + 1) + " on contour.");
                    MINUITPlugin.logStatic("MnContours finds only " + i + " points.");
                    return new ContoursError(px, py, result, mex, mey, nfcn);
                }
                sca = -1.;
            }
        }

        return new ContoursError(px, py, result, mex, mey, nfcn);
    }

    /**
     * <p>points.</p>
     *
     * @param px a int.
     * @param py a int.
     * @return a {@link java.util.List} object.
     */
    public List<Range> points(int px, int py) {
        return points(px, py, 1);
    }

    /**
     * <p>points.</p>
     *
     * @param px a int.
     * @param py a int.
     * @param errDef a double.
     * @return a {@link java.util.List} object.
     */
    public List<Range> points(int px, int py, double errDef) {
        return points(px, py, errDef, 20);
    }

    /**
     * Calculates one function contour of FCN with respect to parameters parx
     * and pary. The return value is a list of (x,y) points. FCN minimized
 always with respect to all other n - 2 variable parameters (if any).
 MINUITPlugin will try to find n points on the contour (default 20). To
 calculate more than one contour, the user needs to set the error
 definition in its FCN to the appropriate value for the desired confidence
 level and call this method for each contour.
     *
     * @param npoints a int.
     * @param px a int.
     * @param py a int.
     * @param errDef a double.
     * @return a {@link java.util.List} object.
     */
    public List<Range> points(int px, int py, double errDef, int npoints) {
        ContoursError cont = contour(px, py, errDef, npoints);
        return cont.points();
    }

    MnStrategy strategy() {
        return theStrategy;
    }
}
