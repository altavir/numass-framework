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
package inr.numass.data;

import hep.dataforge.data.DataPoint;
import hep.dataforge.data.ListDataSet;
import hep.dataforge.datafitter.ParamSet;
import hep.dataforge.datafitter.models.Generator;
import hep.dataforge.datafitter.models.XYModel;
import static hep.dataforge.maths.RandomUtils.getDefaultRandomGenerator;
import static java.lang.Double.isNaN;
import static java.lang.Math.sqrt;
import java.util.Iterator;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Генератор наборов данных для спектров. На входе требуется набор данных,
 * содержащих X-ы и время набора. Могут быть использованы реальные
 * экспериментальные наборы данных.
 *
 * @author Darksnake
 */
public class SpectrumGenerator implements Generator {

    static final double POISSON_BOUNDARY = 100;
    private GeneratorType genType = GeneratorType.POISSONIAN;
    private RandomDataGenerator generator;
    private ParamSet params;
    private XYModel source;
    private SpectrumDataAdapter adapter = new SpectrumDataAdapter();

    public SpectrumGenerator(XYModel source, ParamSet params, int seed) {
        this.source = source;
        this.params = params;
        RandomGenerator rng = new JDKRandomGenerator();
        rng.setSeed(seed);
        this.generator = new RandomDataGenerator(rng);
    }

    public SpectrumGenerator(XYModel source, ParamSet params, RandomGenerator rng) {
        this.source = source;
        this.params = params;
        this.generator = new RandomDataGenerator(rng);
    }

    public SpectrumGenerator(XYModel source, ParamSet params) {
        this(source, params, getDefaultRandomGenerator());
    }

    @Override
    public ListDataSet generateData(Iterable<DataPoint> config) {
        ListDataSet res = adapter.buildEmptyDataSet("");
        for (Iterator<DataPoint> it = config.iterator(); it.hasNext();) {
            res.add(this.generateDataPoint(it.next()));
        }
        return res;
    }

    @Override
    public DataPoint generateDataPoint(DataPoint configPoint) {
        double mu = this.getMu(configPoint);
        if (isNaN(mu) || (mu < 0)) {
            throw new IllegalStateException("Negative input parameter for generator.");
        }
        double y;
        switch (this.genType) {
            case GAUSSIAN:
                double sigma = sqrt(mu);
                if (mu == 0) {
                    y = 0;// Проверяем чтобы не было сингулярности
                } else {
                    y = generator.nextGaussian(mu, sigma);
                }
                if (y < 0) {
                    y = 0;//Проверяем, чтобы не было отрицательных значений
                }
                break;
            case POISSONIAN:
                if (mu == 0) {
                    y = 0;
                    break;
                }
                if (mu < POISSON_BOUNDARY) {
                    y = generator.nextPoisson(mu);
                } else {
                    y = generator.nextGaussian(mu, sqrt(mu));
                }
                break;
            default:
                throw new Error("Enum listing failed!");
        }

        double time = this.getTime(configPoint);

        return adapter.buildSpectrumDataPoint(this.getX(configPoint), (long)y, time);
    }

    @Override
    public String getGeneratorType() {
        return this.genType.name();
    }

    private double getMu(DataPoint point) {
        return source.value(this.getX(point), params) * this.getTime(point);
    }

//    private double getSigma(DataPoint point) {
//        if (!point.containsName("time")) {
//            GlobalContext.instance().logString("SpectrumGenerator : Neither point error nor time is defined. Suspected wrong error bars for data.");
//        }
//        return sqrt(this.getMu(point));
//    }

    private double getTime(DataPoint point) {
        
        return adapter.getTime(point);
//        if (point.containsName("time")) {
//            return point.getValue("time").doubleValue();
//        } else {
//            /*
//             * Это сделано на тот случай, если требуется сгенерить не количество
//             * отсчетов, а скорость счета. Правда в этом случе требуется
//             * передача веса для гауссовского генератора
//             */
//            return 1;
//        }

    }

    public SpectrumDataAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(SpectrumDataAdapter adapter) {
        this.adapter = adapter;
    }

    private double getX(DataPoint point) {
        return adapter.getX(point).doubleValue();
    }

    public void setGeneratorType(GeneratorType type) {
        this.genType = type;
    }

    public enum GeneratorType {

        POISSONIAN,
        GAUSSIAN
    }
}
