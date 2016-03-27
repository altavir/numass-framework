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
package inr.numass.models;

import hep.dataforge.actions.ActionUtils;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.io.ColumnedDataReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.points.DataPoint;
import hep.dataforge.points.PointSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;

/**
 *
 * @author darksnake
 */
public class TransmissionInterpolator implements UnivariateFunction {

    public static TransmissionInterpolator fromFile(Context context, String path, String xName, String yName, int nSmooth, double w, double border) {
        try {
            File dataFile = context.io().getFile(path);
            ColumnedDataReader reader = new ColumnedDataReader(dataFile);
            return new TransmissionInterpolator(reader, xName, yName, nSmooth, w, border);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static TransmissionInterpolator fromAction(Context context, Meta actionAnnotation, 
            String xName, String yName, int nSmooth, double w, double border) throws InterruptedException {
        DataNode<PointSet> node = ActionUtils.runConfig(context, actionAnnotation);
        PointSet data = node.getData().get();
        return new TransmissionInterpolator(data, xName, yName, nSmooth, w, border);
    }

    UnivariateFunction func;
    double[] x;
    double[] y;
    private double xmax;
    private double xmin;

    private TransmissionInterpolator(Iterable<DataPoint> data, String xName, String yName, int nSmooth, double w, double border) {
        prepareXY(data, xName, yName);
        double[] smoothed = smoothXY(x, y, w, border);
        //Циклы сглаживания
        for (int i = 1; i < nSmooth; i++) {
            smoothed = smoothXY(x, smoothed, w, border);
        }
        this.func = new LinearInterpolator().interpolate(x, smoothed);
    }

    public double[] getX() {
        return x;
    }

    /**
     * @return the xmax
     */
    public double getXmax() {
        return xmax;
    }

    /**
     * @return the xmin
     */
    public double getXmin() {
        return xmin;
    }

    public double[] getY() {
        return y;
    }

    /**
     * Prepare and normalize data for interpolation
     *
     * @param data
     * @param xName
     * @param yName
     */
    private void prepareXY(Iterable<DataPoint> data, String xName, String yName) {

        List<DataPoint> points = new ArrayList<>();

        for (DataPoint dp : data) {
            points.add(dp);
        }

        x = new double[points.size()];
        y = new double[points.size()];

        xmin = Double.POSITIVE_INFINITY;
        xmax = Double.NEGATIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.size(); i++) {
            x[i] = points.get(i).getDouble(xName);
            y[i] = points.get(i).getDouble(yName);
            if (x[i] < xmin) {
                xmin = x[i];
            }
            if (x[i] > xmax) {
                xmax = x[i];
            }
            if (y[i] < ymin) {
                ymin = y[i];
            }
            if (y[i] > ymax) {
                ymax = y[i];
            }
        }

//        ymax = y[0]-ymin;
        for (int i = 0; i < y.length; i++) {
            y[i] = (y[i] - ymin) / (ymax - ymin);
        }
    }

    private static double[] smoothXY(double x[], double[] y, double w, double border) {
        int max = y.length - 1;

        double[] yUp = new double[y.length];
        double[] yDown = new double[y.length];

        /* экспоненциальное скользящее среднее 
         /https://ru.wikipedia.org/wiki/%D0%A1%D0%BA%D0%BE%D0%BB%D1%8C%D0%B7%D1%8F%D1%89%D0%B0%D1%8F_%D1%81%D1%80%D0%B5%D0%B4%D0%BD%D1%8F%D1%8F
         /            \textit{EMA}_t  = \alpha \cdot p_t + (1-\alpha) \cdot \textit{EMA}_{t-1},
         */
        yUp[0] = y[0];
        for (int i = 1; i < y.length; i++) {
            if (x[i] < border) {
                yUp[i] = w * y[i] + (1 - w) * yUp[i - 1];
            } else {
                yUp[i] = y[i];
            }
        }

        yDown[max] = y[max];
        for (int i = max - 1; i >= 0; i--) {
            if (x[i] < border) {
                yDown[i] = w * y[i] + (1 - w) * yUp[i + 1];
            } else {
                yDown[i] = y[i];
            }
        }

        double[] res = new double[y.length];
        for (int i = 0; i < x.length; i++) {
            res[i] = (yUp[i] + yDown[i]) / 2;
        }
        return res;
    }

    @Override
    public double value(double x) {
        if (x <= getXmin()) {
            return 1d;
        }
        if (x >= getXmax()) {
            return 0;
        }
        return func.value(x);
    }

}
