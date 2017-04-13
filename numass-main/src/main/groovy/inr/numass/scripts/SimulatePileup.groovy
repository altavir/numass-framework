/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.grind.Grind
import hep.dataforge.tables.DataPoint
import inr.numass.storage.NMPoint
import inr.numass.storage.NumassDataLoader
import inr.numass.storage.RawNMPoint
import inr.numass.utils.NMEventGeneratorWithPulser
import inr.numass.utils.PileUpSimulator
import inr.numass.utils.TritiumUtils
import inr.numass.utils.UnderflowCorrection
import org.apache.commons.math3.random.JDKRandomGenerator

rnd = new JDKRandomGenerator();

////Loading data
File dataDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_1\\set_28")
//File dataDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_2_wide\\set_7")
if (!dataDir.exists()) {
    println "dataDir directory does not exist"
}
def data = NumassDataLoader.fromLocalDir(null, dataDir).getNMPoints()

//File rootDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_1")
////File rootDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_2_wide")
////File rootDir = new File("D:\\Work\\Numass\\data\\2017_01\\Fill_2_wide")
//
//NumassStorage storage = NumassStorage.buildLocalNumassRoot(rootDir, true);
//
//Collection<NMPoint> data = NumassDataUtils.joinSpectra(
//        StorageUtils.loaderStream(storage)
//                .filter { it.key.matches("set_3.") }
//                .map {
//            println "loading ${it.key}"
//            it.value
//        }
//)

//Simulation process
Map<String, List<NMPoint>> res = [:]

List<NMPoint> generated = new ArrayList<>();
List<NMPoint> registered = new ArrayList<>();
List<NMPoint> firstIteration = new ArrayList<>();
List<NMPoint> secondIteration = new ArrayList<>();
List<NMPoint> pileup = new ArrayList<>();

lowerChannel = 400;
upperChannel = 1800;

PileUpSimulator buildSimulator(NMPoint point, double cr, NMPoint reference = null, boolean extrapolate = true, double scale = 1d) {
    def cfg = Grind.buildMeta(cr: cr) {
        pulser(mean: 3450, sigma: 86.45, freq: 66.43)
    }
    NMEventGeneratorWithPulser generator = new NMEventGeneratorWithPulser(rnd, cfg)

    if (extrapolate) {
        double[] chanels = new double[RawNMPoint.MAX_CHANEL];
        double[] values = new double[RawNMPoint.MAX_CHANEL];
        DataPoint fitResult = new UnderflowCorrection().fitPoint(point, 400, 600, 1800, 20); numa

        def amp = fitResult.getDouble("amp")
        def sigma = fitResult.getDouble("expConst")
        if (sigma > 0) {

        for (int i = 0; i < upperChannel; i++) {
            chanels[i] = i;
            if (i < lowerChannel) {
                values[i] = point.getLength()*amp * Math.exp((i as double) / sigma)
            } else {
                values[i] = Math.max(0, point.getCount(i) - (reference == null ? 0 : reference.getCount(i)));
            }
        }
        generator.loadSpectrum(chanels, values)
        } else {
            generator.loadSpectrum(point, reference, lowerChannel, upperChannel);
        }
    } else {
        generator.loadSpectrum(point, reference, lowerChannel, upperChannel);
    }

    return new PileUpSimulator(point.length * scale, rnd, generator).withUset(point.voltage).generate();
}

double adjustCountRate(PileUpSimulator simulator, NMPoint point) {
    double generatedInChannel = simulator.generated().getCountInWindow(lowerChannel, upperChannel);
    double registeredInChannel = simulator.registered().getCountInWindow(lowerChannel, upperChannel);
    return (generatedInChannel / registeredInChannel) * (point.getCountInWindow(lowerChannel, upperChannel) / point.getLength());
}

data.forEach { point ->
    double cr = TritiumUtils.countRateWithDeadTime(point, lowerChannel, upperChannel, 6.55e-6);

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
res.put("original", data);
res.put("generated", generated);
res.put("registered", registered);
//    res.put("firstIteration", new SimulatedPoint("firstIteration", firstIteration));
//    res.put("secondIteration", new SimulatedPoint("secondIteration", secondIteration));
res.put("pileup", pileup);

def keys = res.keySet();

//print spectra for selected point
double u = 16500d;

List<Map> points = res.values().collect { it.find { it.voltage == u }.getMap(20, true) }

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

for (int i = 0; i < data.size(); i++) {
    print "${data.get(i).getVoltage()}\t"
    print "${data.get(i).getLength()}\t"
    print keys.collect { res[it].get(i).getTotalCount() }.join("\t") + "\t"
    print keys.collect { res[it].get(i).getCountInWindow(3100, 3800) }.join("\t") + "\t"
    println keys.collect { res[it].get(i).getCountInWindow(400, 3100) }.join("\t")
}
