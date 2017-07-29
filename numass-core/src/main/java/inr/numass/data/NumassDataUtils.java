package inr.numass.data;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.TableFormatBuilder;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.api.NumassSet;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static hep.dataforge.tables.XYAdapter.*;
import static inr.numass.data.api.NumassAnalyzer.*;

/**
 * Created by darksnake on 30-Jan-17.
 */
public class NumassDataUtils {

    public static NumassSet join(String name, Collection<NumassSet> sets) {
        return new NumassSet() {
            @Override
            public Stream<NumassPoint> getPoints() {
                return sets.stream().flatMap(set -> getPoints());
            }

            @Override
            public Meta meta() {
                MetaBuilder metaBuilder = new MetaBuilder("meta");
                sets.forEach(set -> metaBuilder.putNode(set.getName(), set.meta()));
                return metaBuilder;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    /**
     * Subtract reference spectrum.
     *
     * @param sp1
     * @param sp2
     * @return
     */
    public static Table subtractSpectrum(Table sp1, Table sp2) {
        TableFormat format = new TableFormatBuilder()
                .addNumber(CHANNEL_KEY, X_VALUE_KEY)
                .addNumber(COUNT_RATE_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
                .build();
        ListTable.Builder builder = new ListTable.Builder(format);
        for (int i = 0; i < sp1.size(); i++) {
            Values row1 = sp1.getRow(i);
            Optional<Values> row2 = sp2.getRows().filter(row -> row.getDouble(CHANNEL_KEY) == row1.getDouble(CHANNEL_KEY)).findFirst();

            double value1 = row1.getDouble(COUNT_RATE_KEY);
            double value2 = row2.map(it -> it.getDouble(COUNT_RATE_KEY)).orElse(0d);

            double error1 = row1.getDouble(COUNT_RATE_ERROR_KEY);
            double error2 = row2.map(it-> it.getDouble(COUNT_RATE_ERROR_KEY)).orElse(0d);

            double value = value1 - value2;
            double error = Math.sqrt(error1*error1 + error2*error2);
            builder.row(sp1.get(CHANNEL_KEY, i).intValue(), value, error);
        }
        return builder.build();
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
//        int[] newArray = new int[first.getSpectrum().length];
//        Arrays.setAll(newArray, i -> first.getSpectrum()[i] + second.getSpectrum()[i]);
//        return new NumassPointImpl(
//                first.getVoltage(),
//                Instant.EPOCH,
//                first.getLength() + second.getLength(),
//                newArray
//        );
//    }
//
//    public static NumassPoint substractPoint(NumassPoint point, NumassPoint reference) {
//        int[] array = new int[point.getSpectrum().length];
//        Arrays.setAll(array, i -> Math.max(0, point.getSpectrum()[i] - reference.getSpectrum()[i]));
//        return new NumassPointImpl(
//                point.getVoltage(),
//                point.getStartTime(),
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
//        SpectrumDataAdapter reader = adapter();
//        ListTable.Builder res = new ListTable.Builder(data.getFormat());
//        for (Values dp : data) {
//            double corrFactor = 1 + beta;
//            res.row(reader.buildSpectrumDataPoint(reader.getX(dp).doubleValue() * corrFactor, reader.getCount(dp), reader.getTime(dp)));
//        }
//        return res.build();
//    }
//
    public static SpectrumDataAdapter adapter() {
        return new SpectrumDataAdapter("Uset", "CR", "CRerr", "Time");
    }
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
//    public static Table correctForDeadTime(ListTable data, SpectrumDataAdapter adapter, double dtime) {
////        SpectrumDataAdapter adapter = adapter();
//        ListTable.Builder res = new ListTable.Builder(data.getFormat());
//        for (Values dp : data) {
//            double corrFactor = 1 / (1 - dtime * adapter.getCount(dp) / adapter.getTime(dp));
//            res.row(adapter.buildSpectrumDataPoint(adapter.getX(dp).doubleValue(), (long) (adapter.getCount(dp) * corrFactor), adapter.getTime(dp)));
//        }
//        return res.build();
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
