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
package inr.numass.debunch;

import inr.numass.data.RawNMPoint;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author Darksnake
 */
public class FrameAnalizer implements Debuncher {

    private static final double POISSON_THRESHOLD = 1e-80;

    static double getGaussianCRThreshold(double cr, double frameLength, double prob) {
        double[] xs = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        double[] probs = {1.15e-19, 6.22e-16, 1.27e-12, 9.86e-10, 2.86e-7, 3.167e-5, 0.001349, 0.0227, 0.1586};
        LinearInterpolator interpolator = new LinearInterpolator();
        UnivariateFunction function = interpolator.interpolate(probs, xs);
        double sigmas = function.value(prob);
        return cr + sigmas * Math.sqrt(cr / frameLength);
    }
    //double frameShift;
    double frameLength;
    int lowerChanel = 0;

    int numCircles = 1;
    double rejectionProb;
    int upperChanel = RawNMPoint.MAX_CHANEL;

    

    public FrameAnalizer(double rejectionProb, double frameLength) {
        this.rejectionProb = rejectionProb;
        this.frameLength = frameLength;
    }
    
    public FrameAnalizer(double rejectionProb, double frameLength, int lower, int upper) {
        assert upper > lower;
        this.rejectionProb = rejectionProb;
        this.frameLength = frameLength;
        this.lowerChanel = lower;
        this.upperChanel = upper;
    }    

    public FrameAnalizer(double rejectionProb, double frameLength, int numCircles) {
        this.rejectionProb = rejectionProb;
        this.frameLength = frameLength;
        this.numCircles = numCircles;
    }

    /**
     * Полный аналог Сережиной программы
     *
     * @param numCicles
     * @param prob
     * @param frameShift
     * @param frameLength
     * @return
     */
    private DebunchReport cicledDebunch(RawNMPoint point, int numCicles, double prob, double frameShift, double frameLength) {
        DebunchReport res = this.debunch(point, prob, frameShift, frameLength);
        for (int i = 0; i < numCicles-1; i++) {
            res = this.debunch(res, prob, frameShift, frameLength);
        }
        return res;
    }

    private DebunchReport debunch(DebunchReport res, double prob, double frameShift, double frameLength) {
        return debunch(res.getPoint(), prob, frameShift, frameLength);
    }

    private DebunchReport debunch(RawNMPoint point, double prob, double frameShift, double frameLength) {
        double cr = point.selectChanels(lowerChanel, upperChanel).getCr();
        return debunch(point, cr, prob, frameShift, frameLength);
    }

    private DebunchReport debunch(RawNMPoint point, double averageCR, double prob, double frameShift, double frameLength) {
        
        DebunchData data = new DebunchData(point);

        double timeTotal = data.getLength();
//        long countTotal = data.size();

        double curPos = 0;
        double baseThreshold = getCRThreshold(averageCR, frameLength, prob);
        Frame workFrame;
        boolean bunchFlag = false;// Флаг символизирует, находимся ли мы в состоянии пачки

        while (curPos < (data.getLength() - frameLength)) {
            workFrame = data.countInFrameFast(curPos, frameLength,lowerChanel,upperChanel);

            if (workFrame.getCountRate() > baseThreshold) {
                /*
                * Если счет в рамке превышает порог, то выкидываем рамку из результата и сдвигаем
                * каретку на один шаг. При этом выставляем флаг.
                * Если видим флаг,то вырезаем только последний шаг, чтобы избежать двойного вырезания
                */
                
                if (bunchFlag) {
                    /*Тут возможен косяк, когда две пачки рядом, но не вплотную. Можно сделать
                    * так, чтобы запоминалось не состояние флага, а конец последнего вырезанного кадра
                    */
                    workFrame = data.countInFrameFast(curPos + frameLength - frameShift, frameShift,lowerChanel,upperChanel);
                }

                data.setAsBunch(workFrame);
                timeTotal -= workFrame.length();
                if (timeTotal <= 0) {
                    throw new RuntimeException("Total time after cleaning is zero.");
                }

                bunchFlag = true;

            } else {
                /*
                * Если пачки нет, то просто сдвигаем каретку к следующей рамке и убираем флаг
                */
                bunchFlag = false;
            }
            curPos += frameShift;
        }
        return new DebunchReportImpl(point, data);
    }

    @Override
    public DebunchReport debunchPoint(RawNMPoint point) {
        return cicledDebunch(point, numCircles, rejectionProb, frameLength/4, frameLength);
    }
    
    private double getCRThreshold(double cr, double frameLength, double prob) {
        if (cr * frameLength > 20) {
            return getGaussianCRThreshold(cr, frameLength, prob);
        } else {
            return getPoissonThreshold(cr * frameLength, prob) / frameLength;
        }
    }

    /**
     * Returns set of intervals begining with frameStarts[i]. All FrameStart
     * should be inside data region
     *
     * @param frameStarts
     * @param frameLength
     * @param fast
     * @return
     */
    private Frame[] getIntervals(DebunchData data, double[] frameStarts, double frameLength, boolean fast) {
        Frame[] res = new Frame[frameStarts.length];
        
        for (int i = 0; i < frameStarts.length; i++) {
            if (fast) {
                res[i] = data.countInFrameFast(frameStarts[i], frameLength,lowerChanel,upperChanel);
            } else {
                res[i] = data.countInFrame(frameStarts[i], frameLength,lowerChanel,upperChanel);
            }
        }
        return res;
    }

    /**
     * Returns count rate in consequent frames with the length of frameLength.
     * The last frame could be shorter than the overs. This method could be used
     * for fast distribution calculation.
     *
     * @param frameLength
     * @return
     */
    private double[] getNonIntercectFramesCountRate(DebunchData data, double frameLength) {
        double dataLength = data.getLength();
        int maxFramesCount = (int) Math.ceil(dataLength / frameLength);
        if (maxFramesCount < 2) {
            throw new IllegalArgumentException("The frameLength is too large.");
        }
        
        double[] res = new double[maxFramesCount];
        double frameBegin;
        
        for (int i = 0; i < res.length; i++) {
            frameBegin = i * frameLength;
            res[i] = data.countInFrameFast(frameBegin, frameLength,lowerChanel,upperChanel).getCountRate();
            
        }
        return res;
        
    }

    private int getPoissonThreshold(double mean, double prob) {
        /*
        * Находим точку "обнуления" распределения и значения коммулятивной плотности в этой точке.
        */
        double pdf = FastMath.exp(-mean);
        double cdf = pdf;
        int k = 0;
        while (pdf > POISSON_THRESHOLD) {
            k++;
            pdf *= mean / k;
            cdf += pdf;
        }
        /*
        * Начинаем считать комулятивную плотность в обратном порядке
        */
        cdf = 1 - cdf;
        if (pdf <= 0) {
            throw new Error();// Проверяем чтобы там точно не было нуля;
        }
        while (cdf < prob) {
            k--;
            pdf *= k / mean;
            cdf += pdf;
        }
        return k;
    }

    private Frame[] getUniformShiftedIntervals(DebunchData data, double frameShift, double frameLength, boolean fast) {
        double dataLength = data.getLength();
        int maxFramesCount = (int) Math.ceil(dataLength / frameShift);
        
        double[] frameStarts = new double[maxFramesCount];
        for (int i = 0; i < frameStarts.length; i++) {
            frameStarts[i] = i * frameShift;
            
        }
        return getIntervals(data, frameStarts, frameLength, fast);
    }
}
