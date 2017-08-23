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
package inr.numass.utils;

import hep.dataforge.io.envelopes.EnvelopeBuilder;
import hep.dataforge.io.envelopes.TaglessEnvelopeType;
import hep.dataforge.io.markup.Markedup;
import hep.dataforge.io.markup.SimpleMarkupRenderer;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.Values;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.Math.*;

/**
 * @author Darksnake
 */
public class NumassUtils {

    /**
     * Integral beta spectrum background with given amplitude (total count rate
     * from)
     *
     * @param amplitude
     * @return
     */
    public static UnivariateFunction tritiumBackgroundFunction(double amplitude) {

        return (e) -> {
            /*чистый бета-спектр*/
            double e0 = 18575d;
            double D = e0 - e;//E0-E
            if (D <= 0) {
                return 0;
            }
            return amplitude * factor(e) * D * D;
        };
    }

    private static double factor(double E) {
        double me = 0.511006E6;
        double Etot = E + me;
        double pe = sqrt(E * (E + 2d * me));
        double ve = pe / Etot;
        double yfactor = 2d * 2d * 1d / 137.039 * Math.PI;
        double y = yfactor / ve;
        double Fn = y / abs(1d - exp(-y));
        double Fermi = Fn * (1.002037 - 0.001427 * ve);
        double res = Fermi * pe * Etot;
        return res * 1E-23;
    }


    /**
     * Evaluate groovy expression using numass point as parameter
     *
     * @param expression
     * @param point
     * @return
     */
    public static double pointExpression(String expression, Values point) {
        Map<String, Object> exprParams = new HashMap<>();
        //Adding all point values to expression parameters
        point.getNames().forEach(name -> exprParams.put(name, point.getValue(name).value()));
        //Adding aliases for commonly used parameters
        exprParams.put("T", point.getDouble("length"));
        exprParams.put("U", point.getDouble("voltage"));

        return ExpressionUtils.function(expression, exprParams);
    }

    /**
     * Write an envelope wrapping given data to given stream
     *
     * @param stream
     * @param meta
     * @param dataWriter
     * @throws IOException
     */
    public static void writeEnvelope(OutputStream stream, Meta meta, Consumer<OutputStream> dataWriter) {
        //TODO replace by text envelope when it is ready
        try {
            TaglessEnvelopeType.instance.getWriter().write(
                    stream,
                    new EnvelopeBuilder()
                            .setMeta(meta)
                            .setData(dataWriter)
                            .build()
            );
            stream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeSomething(OutputStream stream, Meta meta, Markedup something) {
        writeEnvelope(stream, meta, out -> new SimpleMarkupRenderer(out).render(something.markup(meta)));
    }


}
