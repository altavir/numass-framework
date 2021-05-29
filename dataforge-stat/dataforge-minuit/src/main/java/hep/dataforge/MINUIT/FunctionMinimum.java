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

import java.util.ArrayList;
import java.util.List;

/**
 * Result of the minimization.
 * <p>
 * The FunctionMinimum is the output of the minimizers and contains the
 * minimization result. The methods
 * <ul>
 * <li>userState(),
 * <li>userParameters() and
 * <li>userCovariance()
 * </ul>
 * are provided. These can be used as new input to a new minimization after some
 * manipulation. The parameters and/or the FunctionMinimum can be printed using
 * the toString() method or the MnPrint class.
 *
 * @author Darksnake
 */
public class FunctionMinimum {

    private boolean theAboveMaxEdm;
    private double theErrorDef;
    private boolean theReachedCallLimit;
    private MinimumSeed theSeed;
    private List<MinimumState> theStates;
    private MnUserParameterState theUserState;

    FunctionMinimum(MinimumSeed seed, double up) {
        theSeed = seed;
        theStates = new ArrayList<>();
        theStates.add(new MinimumState(seed.parameters(), seed.error(), seed.gradient(), seed.parameters().fval(), seed.nfcn()));
        theErrorDef = up;
        theUserState = new MnUserParameterState();
    }

    FunctionMinimum(MinimumSeed seed, List<MinimumState> states, double up) {
        theSeed = seed;
        theStates = states;
        theErrorDef = up;
        theUserState = new MnUserParameterState();
    }

    FunctionMinimum(MinimumSeed seed, List<MinimumState> states, double up, MnReachedCallLimit x) {
        theSeed = seed;
        theStates = states;
        theErrorDef = up;
        theReachedCallLimit = true;
        theUserState = new MnUserParameterState();
    }

    FunctionMinimum(MinimumSeed seed, List<MinimumState> states, double up, MnAboveMaxEdm x) {
        theSeed = seed;
        theStates = states;
        theErrorDef = up;
        theAboveMaxEdm = true;
        theReachedCallLimit = false;
        theUserState = new MnUserParameterState();
    }

    // why not
    void add(MinimumState state) {
        theStates.add(state);
    }

    /**
     * returns the expected vertical distance to the minimum (EDM)
     *
     * @return a double.
     */
    public double edm() {
        return lastState().edm();
    }

    MinimumError error() {
        return lastState().error();
    }

    /**
     * <p>
     * errorDef.</p>
     *
     * @return a double.
     */
    public double errorDef() {
        return theErrorDef;
    }

    /**
     * Returns the function value at the minimum.
     *
     * @return a double.
     */
    public double fval() {
        return lastState().fval();
    }

    FunctionGradient grad() {
        return lastState().gradient();
    }

    boolean hasAccurateCovar() {
        return state().error().isAccurate();
    }

    boolean hasCovariance() {
        return state().error().isAvailable();
    }

    boolean hasMadePosDefCovar() {
        return state().error().isMadePosDef();
    }

    boolean hasPosDefCovar() {
        return state().error().isPosDef();
    }

    boolean hasReachedCallLimit() {
        return theReachedCallLimit;
    }

    boolean hasValidCovariance() {
        return state().error().isValid();
    }

    boolean hasValidParameters() {
        return state().parameters().isValid();
    }

    boolean hesseFailed() {
        return state().error().hesseFailed();
    }

    boolean isAboveMaxEdm() {
        return theAboveMaxEdm;
    }

    /**
     * In general, if this returns <CODE>true</CODE>, the minimizer did find a
     * minimum without running into troubles. However, in some cases a minimum
     * cannot be found, then the return value will be <CODE>false</CODE>.
     * Reasons for the minimization to fail are
     * <ul>
     *  <li>the number of allowed function calls has been exhausted</li>
     *  <li>the minimizer could not improve the values of the parameters (and
     * knowing that it has not converged yet)</li>
     *  <li>a problem with the calculation of the covariance matrix</li>
     * </ul>
     * Additional methods for the analysis of the state at the minimum are
     * provided.
     *
     * @return a boolean.
     */
    public boolean isValid() {
        return state().isValid() && !isAboveMaxEdm() && !hasReachedCallLimit();
    }

    private MinimumState lastState() {
        return theStates.get(theStates.size() - 1);
    }
    // forward interface of last state

    /**
     * returns the total number of function calls during the minimization.
     *
     * @return a int.
     */
    public int nfcn() {
        return lastState().nfcn();
    }

    MinimumParameters parameters() {
        return lastState().parameters();
    }

    MinimumSeed seed() {
        return theSeed;
    }

    MinimumState state() {
        return lastState();
    }

    List<MinimumState> states() {
        return theStates;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public String toString() {
        return MnPrint.toString(this);
    }

    /**
     * <p>
     * userCovariance.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserCovariance} object.
     */
    public MnUserCovariance userCovariance() {
        if (!theUserState.isValid()) {
            theUserState = new MnUserParameterState(state(), errorDef(), seed().trafo());
        }
        return theUserState.covariance();
    }

    /**
     * <p>
     * userParameters.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserParameters} object.
     */
    public MnUserParameters userParameters() {
        if (!theUserState.isValid()) {
            theUserState = new MnUserParameterState(state(), errorDef(), seed().trafo());
        }
        return theUserState.parameters();
    }

    /**
     * user representation of state at minimum
     *
     * @return a {@link hep.dataforge.MINUIT.MnUserParameterState} object.
     */
    public MnUserParameterState userState() {
        if (!theUserState.isValid()) {
            theUserState = new MnUserParameterState(state(), errorDef(), seed().trafo());
        }
        return theUserState;
    }

    static class MnAboveMaxEdm {
    }

    static class MnReachedCallLimit {
    }
}
