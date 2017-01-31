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

    public static Collection<NMPoint> joinSpectra(Stream<NumassData> spectra) {
        Map<Double, NMPoint> map = new LinkedHashMap<>();
        spectra.forEach(datum -> {
            datum.forEach(point -> {
                double uset = point.getUset();
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
    public static NMPoint join(NMPoint first, NMPoint second) {
        if (first.getUset() != second.getUset()) {
            throw new RuntimeException("Voltage mismatch");
        }
        int[] newArray = new int[first.getSpectrum().length];
        Arrays.setAll(newArray, i -> first.getSpectrum()[i] + second.getSpectrum()[i]);
        return new NMPoint(
                first.getUset(),
                first.getUread(),
                Instant.EPOCH,
                first.getLength() + second.getLength(),
                newArray
        );
    }

    public static NMPoint substractPoint(NMPoint point, NMPoint reference) {
        int[] array = new int[point.getSpectrum().length];
        Arrays.setAll(array, i -> Math.max(0, point.getSpectrum()[i] - reference.getSpectrum()[i]));
        return new NMPoint(
                point.getUset(),
                point.getUread(),
                point.getStartTime(),
                point.getLength(),
                array
        );
    }

    public static Collection<NMPoint> substractReferencePoint(Collection<NMPoint> points, double uset) {
        NMPoint reference = points.stream().filter(it -> it.getUset() == uset).findFirst()
                .orElseThrow(() -> new RuntimeException("Reference point not found"));
        return points.stream().map(it -> substractPoint(it, reference)).collect(Collectors.toList());
    }

}
