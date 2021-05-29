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

import java.util.List;

/**
 * <p>ContoursError class.</p>
 *
 * @author Darksnake
 * @version $Id$
 */
public class ContoursError {
    private int theNFcn;
    private int theParX;
    private int theParY;
    private List<Range> thePoints;
    private MinosError theXMinos;
    private MinosError theYMinos;

    ContoursError(int parx, int pary, List<Range> points, MinosError xmnos, MinosError ymnos, int nfcn) {
        theParX = parx;
        theParY = pary;
        thePoints = points;
        theXMinos = xmnos;
        theYMinos = ymnos;
        theNFcn = nfcn;
    }

    /**
     * <p>nfcn.</p>
     *
     * @return a int.
     */
    public int nfcn() {
        return theNFcn;
    }

    /**
     * <p>points.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Range> points() {
        return thePoints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MnPrint.toString(this);
    }

    /**
     * <p>xMinosError.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MinosError} object.
     */
    public MinosError xMinosError() {
        return theXMinos;
    }

    /**
     * <p>xRange.</p>
     *
     * @return
     */
    public Range xRange() {
        return theXMinos.range();
    }

    /**
     * <p>xmin.</p>
     *
     * @return a double.
     */
    public double xmin() {
        return theXMinos.min();
    }

    /**
     * <p>xpar.</p>
     *
     * @return a int.
     */
    public int xpar() {
        return theParX;
    }

    /**
     * <p>yMinosError.</p>
     *
     * @return a {@link hep.dataforge.MINUIT.MinosError} object.
     */
    public MinosError yMinosError() {
        return theYMinos;
    }

    /**
     * <p>yRange.</p>
     *
     * @return
     */
    public Range yRange() {
        return theYMinos.range();
    }

    /**
     * <p>ymin.</p>
     *
     * @return a double.
     */
    public double ymin() {
        return theYMinos.min();
    }

    /**
     * <p>ypar.</p>
     *
     * @return a int.
     */
    public int ypar() {
        return theParY;
    }
}
