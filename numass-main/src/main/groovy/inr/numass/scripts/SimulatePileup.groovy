/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.grind.Grind
import inr.numass.storage.NMPoint
import inr.numass.storage.NumassData
import inr.numass.storage.NumassDataLoader
import inr.numass.utils.NMEventGenerator
import inr.numass.utils.PileUpSimulator
import inr.numass.utils.TritiumUtils
import org.apache.commons.math3.random.JDKRandomGenerator

rnd = new JDKRandomGenerator();

//Loading data
File dataDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_1\\set_24")
if (!dataDir.exists()) {
    println "dataDir directory does not exist"
}
NumassData data = NumassDataLoader.fromLocalDir(null, dataDir)

//Simulation process
Map<String, List<NMPoint>> res = [:]

List<NMPoint> generated = new ArrayList<>();
List<NMPoint> registered = new ArrayList<>();
List<NMPoint> firstIteration = new ArrayList<>();
List<NMPoint> secondIteration = new ArrayList<>();
List<NMPoint> pileup = new ArrayList<>();

lowerChannel = 400;
upperChannel = 1800;

PileUpSimulator buildSimulator(NMPoint point, double cr, NMPoint reference = null, double scale = 1d) {
    def cfg = Grind.buildMeta(cr: cr) {
        pulser(mean: 3450, sigma: 86.45, freq: 66.43)
    }
//    NMEventGenerator generator = new NMEventGeneratorWithPulser(rnd, cfg)
    NMEventGenerator generator = new NMEventGenerator(rnd, cfg)
    generator.loadSpectrum(point, reference, lowerChannel, upperChannel);
    return new PileUpSimulator(point.length * scale, rnd, generator).withUset(point.uset).generate();
}

double adjustCountRate(PileUpSimulator simulator, NMPoint point) {
    double generatedInChannel = simulator.generated().getCountInWindow(lowerChannel, upperChannel);
    double registeredInChannel = simulator.registered().getCountInWindow(lowerChannel, upperChannel);
    return (generatedInChannel / registeredInChannel) * (point.getCountInWindow(lowerChannel, upperChannel) / point.getLength());
}

data.NMPoints.forEach { point ->
    double cr = TritiumUtils.countRateWithDeadTime(point, lowerChannel, upperChannel, 6.2e-6);

    PileUpSimulator simulator = buildSimulator(point, cr);

    //second iteration to exclude pileup overlap
    NMPoint pileupPoint = simulator.pileup();
    firstIteration.add(simulator.registered());

    //updating count rate
    cr = adjustCountRate(simulator, point);
    simulator = buildSimulator(point, cr, pileupPoint);

    pileupPoint = simulator.pileup();
    secondIteration.add(simulator.registered());

    cr = adjustCountRate(simulator, point);
    simulator = buildSimulator(point, cr, pileupPoint);

    generated.add(simulator.generated());
    registered.add(simulator.registered());
    pileup.add(simulator.pileup());
}
res.put("original", data.NMPoints);
res.put("generated", generated);
res.put("registered", registered);
//    res.put("firstIteration", new SimulatedPoint("firstIteration", firstIteration));
//    res.put("secondIteration", new SimulatedPoint("secondIteration", secondIteration));
res.put("pileup", pileup);

def keys = res.keySet();

//print spectra for selected point
double u = 16500d;

List<Map> points = res.values().collect { it.find { it.uset == u }.getMapWithBinning(20, false) }

println "\n Spectrum example for U = ${u}\n"

print "channel\t"
println keys.join("\t")

points.first().keySet().each {
    print "${it}\t"
    println points.collect { map -> map[it] }.join("\t")
}

//printing count rate in window
print "U\tLength\t"
print keys.collect { it + "[total]" }.join("\t") + "\t"
print keys.collect { it + "[pulse]" }.join("\t") + "\t"
println keys.join("\t")

for (int i = 0; i < data.getNMPoints().size(); i++) {
    print "${data.getNMPoints().get(i).getUset()}\t"
    print "${data.getNMPoints().get(i).getLength()}\t"
    print keys.collect { res[it].get(i).getEventsCount() }.join("\t") + "\t"
    print keys.collect { res[it].get(i).getCountInWindow(3100, 3800) }.join("\t") + "\t"
    println keys.collect { res[it].get(i).getCountInWindow(400, 3100) }.join("\t")
}
