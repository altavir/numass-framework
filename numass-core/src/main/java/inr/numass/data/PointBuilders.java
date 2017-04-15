package inr.numass.data;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by darksnake on 13-Apr-17.
 */
public class PointBuilders {
    public static NumassPoint readProtoPoint(double u, Instant startTime, double pointLength, InputStream stream, Function<NumassProto.Point.Channel.Block.Event, Integer> peakFinder) throws IOException {
        NumassProto.Point point = NumassProto.Point.parseFrom(stream);
        NumassProto.Point.Channel ch = point.getChannels(0);
        int[] spectrum = count(ch.getBlocksList().stream()
                .flatMapToInt(block -> IntStream.concat(
                        block.getPeaks().getAmplitudesList()
                                .stream().mapToInt(it -> it.intValue()),
                        block.getEventsList().stream()
                                .mapToInt(event -> peakFinder.apply(event))
                ))
        );

        return new NMPoint(u, startTime, pointLength, spectrum);
    }

    private static int[] calculateSpectrum(RawNMPoint point) {
        assert point.getEventsCount() > 0;
        return count(point.getEvents().stream().mapToInt(event -> event.getChanel()));
    }

    @NotNull
    public static NumassPoint readRawPoint(@NotNull RawNMPoint point) {
        return new NMPoint(point.getUset(), point.getStartTime(), point.getLength(), calculateSpectrum(point));
    }

    private static int[] count(IntStream stream) {
        List<AtomicInteger> list = new ArrayList<>();
        stream.forEach(i -> {
            while (list.size() <= i) {
                list.add(new AtomicInteger(0));
            }
            list.get(i).incrementAndGet();
        });
        return list.stream().mapToInt(i -> i.get()).toArray();
    }
}
