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

/**
 * <p>MnCross class.</p>
 *
 * @version $Id$
 * @author Darksnake
 */
public class MnCross {
    private boolean theLimset;
    private boolean theMaxFcn;
    private int theNFcn;
    private boolean theNewMin;
    private MnUserParameterState theState;
    private boolean theValid;
    private double theValue;

    MnCross() {
        theState = new MnUserParameterState();
    }

    MnCross(int nfcn) {
        theState = new MnUserParameterState();
        theNFcn = nfcn;
    }

    MnCross(double value, MnUserParameterState state, int nfcn) {
        theValue = value;
        theState = state;
        theNFcn = nfcn;
        theValid = true;
    }

    MnCross(MnUserParameterState state, int nfcn, CrossParLimit x) {
        theState = state;
        theNFcn = nfcn;
        theLimset = true;
    }

    MnCross(MnUserParameterState state, int nfcn, CrossFcnLimit x) {
        theState = state;
        theNFcn = nfcn;
        theMaxFcn = true;
    }

    MnCross(MnUserParameterState state, int nfcn, CrossNewMin x) {
        theState = state;
        theNFcn = nfcn;
        theNewMin = true;
    }

    boolean atLimit() {
        return theLimset;
    }

    boolean atMaxFcn() {
        return theMaxFcn;
    }

    boolean isValid() {
        return theValid;
    }

    boolean newMinimum() {
        return theNewMin;
    }

    int nfcn() {
        return theNFcn;
    }

    MnUserParameterState state() {
        return theState;
    }

    double value() {
        return theValue;
    }

    static class CrossFcnLimit {
    }

    static class CrossNewMin {
    }

    static class CrossParLimit {
    }
}
