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
 *
 * @version $Id$
 */
abstract class MnParabolaFactory {

    static MnParabola create(MnParabolaPoint p1, MnParabolaPoint p2, MnParabolaPoint p3) {
        double x1 = p1.x();
        double x2 = p2.x();
        double x3 = p3.x();
        double dx12 = x1 - x2;
        double dx13 = x1 - x3;
        double dx23 = x2 - x3;

        double xm = (x1 + x2 + x3) / 3.;
        x1 -= xm;
        x2 -= xm;
        x3 -= xm;

        double y1 = p1.y();
        double y2 = p2.y();
        double y3 = p3.y();

        double a = y1 / (dx12 * dx13) - y2 / (dx12 * dx23) + y3 / (dx13 * dx23);
        double b = -y1 * (x2 + x3) / (dx12 * dx13) + y2 * (x1 + x3) / (dx12 * dx23) - y3 * (x1 + x2) / (dx13 * dx23);
        double c = y1 - a * x1 * x1 - b * x1;

        c += xm * (xm * a - b);
        b -= 2. * xm * a;

        return new MnParabola(a, b, c);
    }

    static MnParabola create(MnParabolaPoint p1, double dxdy1, MnParabolaPoint p2) {
        double x1 = p1.x();
        double xx1 = x1 * x1;
        double x2 = p2.x();
        double xx2 = x2 * x2;
        double y1 = p1.y();
        double y12 = p1.y() - p2.y();

        double det = xx1 - xx2 - 2. * x1 * (x1 - x2);
        double a = -(y12 + (x2 - x1) * dxdy1) / det;
        double b = -(-2. * x1 * y12 + (xx1 - xx2) * dxdy1) / det;
        double c = y1 - a * xx1 - b * x1;

        return new MnParabola(a, b, c);
    }
}
