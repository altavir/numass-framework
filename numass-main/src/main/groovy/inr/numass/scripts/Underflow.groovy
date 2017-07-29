/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.grind.Grind
import hep.dataforge.meta.Meta
import hep.dataforge.storage.commons.StorageUtils
import hep.dataforge.tables.Table
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassStorage
import inr.numass.data.storage.NumassStorageFactory

import java.util.stream.Collectors

import static inr.numass.data.api.NumassAnalyzer.CHANNEL_KEY
import static inr.numass.data.api.NumassAnalyzer.COUNT_RATE_KEY

//Defining root directory

//File rootDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_1")
//File rootDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_2_wide")
//File rootDir = new File("D:\\Work\\Numass\\data\\2017_01\\Fill_2_wide")
File rootDir = new File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

//creating storage instance

NumassStorage storage = NumassStorageFactory.buildLocal(rootDir);

//Reading points
Map<Double, List<NumassPoint>> allPoints = StorageUtils
        .loaderStream(storage)
        .filter { it.key.matches("set_.{1,3}") }
        .map {
    println "loading ${it.key}"
    it.value
}.flatMap { it.points }
        .collect(Collectors.groupingBy { it.voltage })

Meta analyzerMeta = Grind.buildMeta(t0: 3e4)
NumassAnalyzer analyzer = new TimeAnalyzer()

//creating spectra
Map spectra = allPoints.collectEntries {
    def point = new SimpleNumassPoint(it.key, it.value)
    println "generating spectrum for ${point.voltage}"
    return [(point.voltage): analyzer.getSpectrum(point, analyzerMeta)]
}



def refereceVoltage = 18600d
int binning = 20

//subtracting reference point
if (refereceVoltage) {
    def referencePoint = spectra[refereceVoltage]
    spectra = spectra.findAll { it.key != refereceVoltage }.collectEntries {
        return [(it.key): NumassDataUtils.subtractSpectrum(it.getValue() as Table, referencePoint as Table)]
    }
}

//Apply binning
if (binning) {
    spectra = spectra.collectEntries {
        [(it.key): NumassAnalyzer.spectrumWithBinning(it.value as Table, binning)];
    }
}

//printing selected points
def printPoint = { Map<Double, Table> points ->
    print "channel"
    points.each { print "\t${it.key}" }
    println()

    def firstPoint = points.values().first()
    (0..firstPoint.size()).each { i ->
        print firstPoint.get(CHANNEL_KEY, i).intValue()
        points.values().each {
            print "\t${it.get(COUNT_RATE_KEY, i).doubleValue()}"
        }
        println()
    }
    println()
}

println "\n# spectra\n"

//printPoint(data, [16200d, 16400d, 16800d, 17000d, 17200d, 17700d])
printPoint(spectra.findAll { it.key in [16200d, 16400d, 16800d, 17000d, 17200d, 17700d] })

println()

//Table t = new UnderflowCorrection().fitAllPoints(data, 350, 550, 3100, 20);
//ColumnedDataWriter.writeTable(System.out, t, "underflow parameters")

//println "Empty files:"
//Collection<NMPoint> emptySpectra = NumassDataUtils.joinSpectra(
//        StorageUtils.loaderStream(storage).filter{ it.key.matches("Empty.*")}.map {
//            println it.key
//            it.value
//        }
//)

//emptySpectra = NumassDataUtils.substractReferencePoint(emptySpectra,18600);
//
//data = data.collect { point ->
//    NMPoint ref = emptySpectra.find { it.u == point.u }
//    if (ref) {
//        println "substracting tritium background for point ${point.u}"
//        NumassDataUtils.substractPoint(point, ref)
//    } else {
//        println "point ${point.u} skipped"
//        point
//    }
//}