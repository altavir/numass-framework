package inr.numass.storage;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by darksnake on 30-Jan-17.
 */
public class NumassDataUtils {

    public static Iterable<NMPoint> sumSpectra(Stream<NumassData> spectra) {
        Map<Double, NMPoint> map = new HashMap<>();
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

    private static NMPoint join(NMPoint first, NMPoint second) {
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
}
