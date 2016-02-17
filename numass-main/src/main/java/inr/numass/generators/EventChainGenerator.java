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
package inr.numass.generators;

import inr.numass.data.NMEvent;
import inr.numass.data.RawNMPoint;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Darksnake
 */
public final class EventChainGenerator {

    private final static double us = 1e-6;

    private double blockStartTime = 0;

    private final List<NMEvent> generatedChain = new ArrayList<>();
    private final EventGenerator generator;
    private final double length;
    private final List<NMEvent> pileupChain = new ArrayList<>();

    private final List<NMEvent> registredChain = new ArrayList<>();

    public EventChainGenerator(double cr, double length) {
        generator = new EventGenerator(cr);
        this.length = length;
        
        run();
    }

    public EventChainGenerator(double cr, double length, RawNMPoint source, int minChanel, int maxChanel) {
        generator = new EventGenerator(cr);
        this.generator.loadFromPoint(source, minChanel, maxChanel);
        this.length = length;
        
        run();
    }
    
    public EventChainGenerator(double cr, double length, Map<Double,Double> spectrum, int minChanel, int maxChanel) {
        generator = new EventGenerator(cr);
        this.generator.loadFromSpectrum(spectrum, minChanel, maxChanel);
        this.length = length;
        
        run();
    }    

    /**
     * Амлпитуда второго сигнала в зависимости от амплитуд наложенных сигналов и
     * задержки
     *
     * @param delay
     * @return
     */
    private short getNewChanel(double delay, short prevChanel, short newChanel) {
        assert delay > 0;
        //эмпирическая формула для канала
        double x = delay / us;
        double coef = max(0, 0.99078 + 0.05098 * x - 0.45775 * x * x + 0.10962 * x * x * x);
        
        return (short) (prevChanel + coef * newChanel);
    }

    public RawNMPoint getPileUp() {
        return new RawNMPoint(2, pileupChain, length);
    }

    public RawNMPoint getPointAsGenerated() {
        return new RawNMPoint(0, generatedChain, length);
    }

    public RawNMPoint getPointAsRegistred() {
        return new RawNMPoint(1, registredChain, length);
    }

    /**
     * Имеется второй сигнал
     *
     * @param delay
     * @return
     */
    private boolean hasNew(double delay) {
        if (delay > 2.65 * us) {
            return false;
        } else if (delay < 2.35 * us) {
            return true;
        } else {
            return heads((2.65 * us - delay) / 0.3 / us);
        }
    }

    private boolean heads(double prob) {
        double r = generator.nextUniform();
        return r < prob;
    }

    NMEvent nextEvent(NMEvent prev) {
        if (prev == null) {
            return generator.nextEvent(new NMEvent((short)0, 0));
        }

        NMEvent event = generator.nextEvent(prev);
        generatedChain.add(event);
        double delay = event.getTime() - blockStartTime;
        if (notDT(delay)) {
            //Если система сбора данных успела переварить предыдущие события
            registredChain.add(event);
            blockStartTime = event.getTime();
            return event;
        } else {
            if ((!prevSurvived(delay)) && (!registredChain.isEmpty())) {
                //если первое событие не выжило, а ушло в наложения
                registredChain.remove(registredChain.size() - 1);
            }
            if (hasNew(delay)) {
                // Если есть событие с увеличенной амлитудой
                NMEvent pileup = new NMEvent(getNewChanel(delay, prev.getChanel(), event.getChanel()), event.getTime());
                registredChain.add(pileup);
                pileupChain.add(pileup);
            }
            //возвращаем предыдущее событие, чтобы отсчитывать мертвое время от него
            return prev;
        }
    }

    /**
     * Не попал в мертвое время и наложения
     *
     * @param delay
     * @return
     */
    private boolean notDT(double delay) {
        if (delay > 7.0 * us) {
            return true;
        } else if (delay < 6.5 * us) {
            return false;
        } else {
            return heads((delay - 6.5 * us) / 0.5 / us);
        }
    }

    /**
     * Выжило предыдушее событие
     *
     * @param delay
     * @return
     */
    private boolean prevSurvived(double delay) {
        if (delay > 2.65 * us) {
            return true;
        } else if (delay < 2.35 * us) {
            return false;
        } else {
            return heads((delay - 2.35 * us) / 0.3 / us);
        }
    }

    private void run() {
        NMEvent next = null;
        
        do {
            next = nextEvent(next);
        } while (next.getTime() < length);
    }

}
