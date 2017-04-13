package inr.numass.storage;

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
        return new NMPoint(
                first.getVoltage(),
                Instant.EPOCH,
                first.getLength() + second.getLength(),
                newArray
        );
    }

    public static NumassPoint substractPoint(NumassPoint point, NumassPoint reference) {
        int[] array = new int[point.getSpectrum().length];
        Arrays.setAll(array, i -> Math.max(0, point.getSpectrum()[i] - reference.getSpectrum()[i]));
        return new NMPoint(
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

}
