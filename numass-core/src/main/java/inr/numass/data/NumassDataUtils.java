package inr.numass.data;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.api.NumassSet;
import inr.numass.data.api.SimpleNumassPoint;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by darksnake on 30-Jan-17.
 */
public class NumassDataUtils {

    public static NumassSet join(String name, Collection<NumassSet> sets) {
        return new NumassSet() {
            @Override
            public Stream<NumassPoint> getPoints() {
                Map<Double,List<NumassPoint>> points =  sets.stream().flatMap(NumassSet::getPoints)
                        .collect(Collectors.groupingBy(NumassPoint::getVoltage));
                return points.entrySet().stream().map(entry->new SimpleNumassPoint(entry.getKey(),entry.getValue()));
            }

            @Override
            public Meta getMeta() {
                MetaBuilder metaBuilder = new MetaBuilder();
                sets.forEach(set -> metaBuilder.putNode(set.getName(), set.getMeta()));
                return metaBuilder;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    @NotNull
    public static SpectrumAdapter adapter() {
        return new SpectrumAdapter("Uset", "CR", "CRerr", "Time");
    }


    //    public static Collection<NumassPoint> joinSpectra(Stream<NumassSet> spectra) {
//        Map<Double, NumassPoint> map = new LinkedHashMap<>();
//        spectra.forEach(datum -> {
//            datum.forEach(point -> {
//                double uset = point.getVoltage();
//                if (map.containsKey(uset)) {
//                    map.put(uset, join(point, map.get(uset)));
//                } else {
//                    map.put(uset, point);
//                }
//            });
//        });
//        return map.values();
//    }
//
//    /**
//     * Spectral sum of two points
//     *
//     * @param first
//     * @param second
//     * @return
//     */
//    public static NumassPoint join(NumassPoint first, NumassPoint second) {
//        if (first.getVoltage() != second.getVoltage()) {
//            throw new RuntimeException("Voltage mismatch");
//        }
//        int[] newArray = new int[first.getAmplitudeSpectrum().length];
//        Arrays.setAll(newArray, i -> first.getAmplitudeSpectrum()[i] + second.getAmplitudeSpectrum()[i]);
//        return new NumassPointImpl(
//                first.getVoltage(),
//                Instant.EPOCH,
//                first.getLength() + second.getLength(),
//                newArray
//        );
//    }
//
//    public static NumassPoint substractPoint(NumassPoint point, NumassPoint reference) {
//        int[] array = new int[point.getAmplitudeSpectrum().length];
//        Arrays.setAll(array, i -> Math.max(0, point.getAmplitudeSpectrum()[i] - reference.getAmplitudeSpectrum()[i]));
//        return new NumassPointImpl(
//                point.getVoltage(),
//                point.getTime(),
//                point.getLength(),
//                array
//        );
//    }
//
//    public static Collection<NumassPoint> substractReferencePoint(Collection<NumassPoint> points, double uset) {
//        NumassPoint reference = points.stream().filter(it -> it.getVoltage() == uset).findFirst()
//                .orElseThrow(() -> new RuntimeException("Reference point not found"));
//        return points.stream().map(it -> substractPoint(it, reference)).collect(Collectors.toList());
//    }
//
//
//    /**
//     * Поправка масштаба высокого.
//     *
//     * @param data
//     * @param beta
//     * @return
//     */
//    public static Table setHVScale(ListTable data, double beta) {
//        SpectrumAdapter reader = adapter();
//        ListTable.Builder res = new ListTable.Builder(data.getFormat());
//        for (Values dp : data) {
//            double corrFactor = 1 + beta;
//            res.row(reader.buildSpectrumDataPoint(reader.getX(dp).doubleValue() * corrFactor, reader.getCount(dp), reader.getTime(dp)));
//        }
//        return res.builder();
//    }
//
//
//    public static Table correctForDeadTime(ListTable data, double dtime) {
//        return correctForDeadTime(data, adapter(), dtime);
//    }
//
//    /**
//     * Коррекция на мертвое время в секундах
//     *
//     * @param data
//     * @param dtime
//     * @return
//     */
//    public static Table correctForDeadTime(ListTable data, SpectrumAdapter adapter, double dtime) {
////        SpectrumAdapter adapter = adapter();
//        ListTable.Builder res = new ListTable.Builder(data.getFormat());
//        for (Values dp : data) {
//            double corrFactor = 1 / (1 - dtime * adapter.getCount(dp) / adapter.getTime(dp));
//            res.row(adapter.buildSpectrumDataPoint(adapter.getX(dp).doubleValue(), (long) (adapter.getCount(dp) * corrFactor), adapter.getTime(dp)));
//        }
//        return res.builder();
//    }
//
//    public static double countRateWithDeadTime(NumassPoint p, int from, int to, double deadTime) {
//        double wind = p.getCountInWindow(from, to) / p.getLength();
//        double res;
//        if (deadTime > 0) {
//            double total = p.getTotalCount();
////            double time = p.getLength();
////            res = wind / (1 - total * deadTime / time);
//            double timeRatio = deadTime / p.getLength();
//            res = wind / total * (1d - Math.sqrt(1d - 4d * total * timeRatio)) / 2d / timeRatio;
//        } else {
//            res = wind;
//        }
//        return res;
//    }
//
//    public static double countRateWithDeadTimeErr(NumassPoint p, int from, int to, double deadTime) {
//        return Math.sqrt(countRateWithDeadTime(p, from, to, deadTime) / p.getLength());
//    }
}
