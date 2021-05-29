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
 * parabola = a*xx + b*x + c
 *
 * @version $Id$
 */
class MnParabola {
    private double theA;
    private double theB;
    private double theC;

    MnParabola(double a, double b, double c) {
        theA = a;
        theB = b;
        theC = c;
    }

    double a() {
        return theA;
    }

    double b() {
        return theB;
    }

    double c() {
        return theC;
    }

    double min() {
        return -theB / (2. * theA);
    }

    double x_neg(double y) {
        return (-Math.sqrt(y / theA + min() * min() - theC / theA) + min());
    }

    double x_pos(double y) {
        return (Math.sqrt(y / theA + min() * min() - theC / theA) + min());
    }

    double y(double x) {
        return (theA * x * x + theB * x + theC);
    }

    double ymin() {
        return (-theB * theB / (4. * theA) + theC);
    }
}
