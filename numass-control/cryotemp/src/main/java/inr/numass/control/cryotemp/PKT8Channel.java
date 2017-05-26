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

package inr.numass.control.cryotemp;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.Metoid;
import hep.dataforge.names.Named;
import hep.dataforge.values.Value;

import java.util.List;
import java.util.function.Function;

/**
 * Created by darksnake on 28-Sep-16.
 */
public class PKT8Channel implements Named, Metoid {

    private final Meta meta;
    private final Function<Double, Double> transformation;

    public PKT8Channel(String name) {
        this.meta = new MetaBuilder("channel")
                .putValue("name", name);
        transformation = (d) -> d;
    }

    public PKT8Channel(Meta meta) {
        this.meta = meta;

        String transformationType = meta.getString("transformationType", "default");
        if (meta.hasValue("coefs")) {
            switch (transformationType) {
                case "default":
                case "hyperbolic":
                    List<Value> coefs = meta.getValue("coefs").listValue();
                    double r0 = meta.getDouble("r0", 1000);
                    transformation = (r) -> {
                        if (coefs == null) {
                            return -1d;
                        } else {
                            double res = 0;
                            for (int i = 0; i < coefs.size(); i++) {
                                res += coefs.get(i).doubleValue() * Math.pow(r0 / r, i);
                            }
                            return res;
                        }
                    };
                    break;
                default:
                    throw new RuntimeException("Unknown transformation type");
            }
        } else {
            //identity transformation
            transformation = (d) -> d;

        }

    }

    @Override
    public String getName() {
        return meta().getString("name");
    }

    @Override
    public Meta meta() {
        return meta;
    }

    public String description() {
        return meta().getString("description", "");
    }

    /**
     * @param r negative if temperature transformation not defined
     * @return
     */
    public double getTemperature(double r) {
        return transformation.apply(r);
    }

    public PKT8Result evaluate(double r) {
        return new PKT8Result(getName(), r, getTemperature(r));
    }

}