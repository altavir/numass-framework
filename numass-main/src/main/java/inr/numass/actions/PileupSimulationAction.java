/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.reports.Reportable;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import inr.numass.storage.NMPoint;
import inr.numass.storage.NumassData;
import inr.numass.storage.RawNMPoint;
import inr.numass.utils.PileUpSimulator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulate pileup
 *
 * @author Alexander Nozik
 */
@TypedActionDef(name = "simulatePileup", inputType = NumassData.class, outputType = Map.class)
public class PileupSimulationAction extends OneToOneAction<NumassData, Map<String, NumassData>> {

    @Override
    protected Map<String, NumassData> execute(Context context, Reportable log, String name, Laminate inputMeta, NumassData input) {
        int lowerChannel = inputMeta.getInt("lowerChannel", 1);
        int upperChannel = inputMeta.getInt("upperChannel", RawNMPoint.MAX_CHANEL-1);

        List<NMPoint> generated = new ArrayList<>();
        List<NMPoint> registered = new ArrayList<>();
        List<NMPoint> firstIteration = new ArrayList<>();
        List<NMPoint> secondIteration = new ArrayList<>();
        List<NMPoint> pileup = new ArrayList<>();

        double scale = inputMeta.getDouble("scale", 1d);

        input.getNMPoints().forEach(point -> {
            double length = point.getLength() * scale;
            double cr = point.getCountRate(lowerChannel, upperChannel, 6.4e-6);

            PileUpSimulator simulator = new PileUpSimulator(cr, length)
                    .withGenerator(point, null, lowerChannel, upperChannel)
                    .generate();

            //second iteration to exclude pileup overlap
            NMPoint pileupPoint = simulator.pileup();
            firstIteration.add(simulator.registered());
            simulator = new PileUpSimulator(cr, length)
                    .withGenerator(point, pileupPoint, lowerChannel, upperChannel)
                    .generate();

            pileupPoint = simulator.pileup();
            secondIteration.add(simulator.registered());
            simulator = new PileUpSimulator(cr, length)
                    .withGenerator(point, pileupPoint, lowerChannel, upperChannel)
                    .generate();

            generated.add(simulator.generated());
            registered.add(simulator.registered());
            pileup.add(simulator.pileup());
        });
        Map<String, NumassData> res = new LinkedHashMap<>();
        res.put("original", input);
        res.put("generated", new SimulatedPoint("generated", generated));
        res.put("registered", new SimulatedPoint("registered", registered));
        res.put("firstIteration", new SimulatedPoint("firstIteration", firstIteration));
        res.put("secondIteration", new SimulatedPoint("secondIteration", secondIteration));
        res.put("pileup", new SimulatedPoint("pileup", pileup));
        return res;
    }

    private static class SimulatedPoint implements NumassData {

        private final String name;
        private final List<NMPoint> points;

        public SimulatedPoint(String name, List<NMPoint> points) {
            this.name = name;
            this.points = points;
        }

        @Override
        public String getDescription() {
            return name;
        }

        @Override
        public Meta meta() {
            return Meta.empty();
        }

        @Override
        public List<NMPoint> getNMPoints() {
            return points;
        }

        @Override
        public boolean isEmpty() {
            return points.isEmpty();
        }

        @Override
        public Instant startTime() {
            return Instant.EPOCH;
        }

        @Override
        public String getName() {
            return name;
        }

    }

}
