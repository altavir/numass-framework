package inr.numass.actions;

import hep.dataforge.actions.ManyToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.meta.Laminate;
import inr.numass.data.NumassPointImpl;

import java.util.Collection;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by darksnake on 04-Nov-16.
 */
@TypedActionDef(name = "joinData", inputType = NumassData.class, outputType = NumassData.class,
        info = "Join a number of numass data files into one single file via spectrum summing")
public class JoinNumassDataAction extends ManyToOneAction<NumassData, NumassData> {

    @Override
    protected NumassData execute(Context context, String nodeName, Map<String, NumassData> input, Laminate meta) {
        throw new UnsupportedOperationException("not implemented");
    }

    private NumassPoint joinPoint(Collection<NumassPoint> points) {
        return points.stream().reduce((p1, p2) -> {
            if (p1.getVoltage() != p2.getVoltage()) {
                throw new RuntimeException("Can't sum points with different Uset");
            }
            return new NumassPointImpl(
                    (p1.getVoltage() + p2.getVoltage()) / 2,
                    p1.getStartTime(),
                    p1.getLength() + p2.getLength(),
                    IntStream.range(0, p1.getSpectrum().length).map(i -> p1.getSpectrum()[i] * p2.getSpectrum()[i]).toArray()
            );
        }).get();
    }
}
