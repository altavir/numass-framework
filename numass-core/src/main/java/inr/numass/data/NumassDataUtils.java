package inr.numass.data;

import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by darksnake on 30-Jan-17.
 */
public class NumassDataUtils {

    public static Collection<NumassPoint> joinSpectra(Stream<NumassData> spectra) {
        Map<Double, NumassPoint> map = new LinkedHashMap<>();
        spectra.forEach(datum -> {
            datum.forEach(point -> {
                double uset = point.getVoltage();
                if (map.containsKey(uset)) {
                    map.put(uset, join(point, map.get(uset)));
                } else {
                    map.put(uset, point);
                }
            });
        });
        return map.values();
    }

    /**
     * Spectral sum of two points
     *
     * @param first
     * @param second
     * @return
     */
    public static NumassPoint join(NumassPoint first, NumassPoint second) {
        if (first.getVoltage() != second.getVoltage()) {
            throw new RuntimeException("Voltage mismatch");
        }
        int[] newArray = new int[first.getSpectrum().length];
        Arrays.setAll(newArray, i -> first.getSpectrum()[i] + second.getSpectrum()[i]);
        return new NumassPointImpl(
                first.getVoltage(),
                Instant.EPOCH,
                first.getLength() + second.getLength(),
                newArray
        );
    }

    public static NumassPoint substractPoint(NumassPoint point, NumassPoint reference) {
        int[] array = new int[point.getSpectrum().length];
        Arrays.setAll(array, i -> Math.max(0, point.getSpectrum()[i] - reference.getSpectrum()[i]));
        return new NumassPointImpl(
                point.getVoltage(),
                point.getStartTime(),
                point.getLength(),
                array
        );
    }

    public static Collection<NumassPoint> substractReferencePoint(Collection<NumassPoint> points, double uset) {
        NumassPoint reference = points.stream().filter(it -> it.getVoltage() == uset).findFirst()
                .orElseThrow(() -> new RuntimeException("Reference point not found"));
        return points.stream().map(it -> substractPoint(it, reference)).collect(Collectors.toList());
    }


    /**
     * Поправка масштаба высокого.
     *
     * @param data
     * @param beta
     * @return
     */
    public static Table setHVScale(ListTable data, double beta) {
        SpectrumDataAdapter reader = adapter();
        ListTable.Builder res = new ListTable.Builder(data.getFormat());
        for (DataPoint dp : data) {
            double corrFactor = 1 + beta;
            res.row(reader.buildSpectrumDataPoint(reader.getX(dp).doubleValue() * corrFactor, reader.getCount(dp), reader.getTime(dp)));
        }
        return res.build();
    }

    public static SpectrumDataAdapter adapter() {
        return new SpectrumDataAdapter("Uset", "CR", "CRerr", "Time");
    }

    public static Table correctForDeadTime(ListTable data, double dtime) {
        return correctForDeadTime(data, adapter(), dtime);
    }

    /**
     * Коррекция на мертвое время в секундах
     *
     * @param data
     * @param dtime
     * @return
     */
    public static Table correctForDeadTime(ListTable data, SpectrumDataAdapter adapter, double dtime) {
//        SpectrumDataAdapter adapter = adapter();
        ListTable.Builder res = new ListTable.Builder(data.getFormat());
        for (DataPoint dp : data) {
            double corrFactor = 1 / (1 - dtime * adapter.getCount(dp) / adapter.getTime(dp));
            res.row(adapter.buildSpectrumDataPoint(adapter.getX(dp).doubleValue(), (long) (adapter.getCount(dp) * corrFactor), adapter.getTime(dp)));
        }
        return res.build();
    }

    public static double countRateWithDeadTime(NumassPoint p, int from, int to, double deadTime) {
        double wind = p.getCountInWindow(from, to) / p.getLength();
        double res;
        if (deadTime > 0) {
            double total = p.getTotalCount();
//            double time = p.getLength();
//            res = wind / (1 - total * deadTime / time);
            double timeRatio = deadTime / p.getLength();
            res = wind / total * (1d - Math.sqrt(1d - 4d * total * timeRatio)) / 2d / timeRatio;
        } else {
            res = wind;
        }
        return res;
    }

    public static double countRateWithDeadTimeErr(NumassPoint p, int from, int to, double deadTime) {
        return Math.sqrt(countRateWithDeadTime(p, from, to, deadTime) / p.getLength());
    }
}
