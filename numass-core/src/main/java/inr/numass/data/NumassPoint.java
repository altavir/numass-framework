package inr.numass.data;

import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.MapPoint;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by darksnake on 13-Apr-17.
 */
public interface NumassPoint {
    String[] dataNames = {"chanel", "count"};

    Instant getStartTime();

    int getCount(int chanel);

    int getCountInWindow(int from, int to);

    List<DataPoint> getData();

    long getTotalCount();

    default Map<Double, Double> getMap(int binning, boolean normalize) {
        Map<Double, Double> res = new LinkedHashMap<>();

        double norm;
        if (normalize) {
            norm = getLength();
        } else {
            norm = 1d;
        }

        int i = 0;

        while (i < getMaxChannel() - binning) {
            int start = i;
            double sum = 0;
            while (i < start + binning) {
                sum += getCount(i);
                i++;
            }
            res.put(start + Math.floor(binning / 2d), sum / norm);
        }
        return res;
    }

    default List<DataPoint> getData(int binning, boolean normalize) {
        return getMap(binning, normalize).entrySet().stream()
                .map(entry -> new MapPoint(dataNames, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    double getLength();

    double getVoltage();

    int[] getSpectrum();

    default int getMaxChannel(){
        return getSpectrum().length - 1;
    }
}
