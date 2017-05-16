/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.storage.commons.StorageUtils
import hep.dataforge.tables.Table
import inr.numass.data.NMPoint
import inr.numass.data.NumassData
import inr.numass.data.NumassDataUtils
import inr.numass.storage.NumassStorage
import inr.numass.utils.UnderflowCorrection

//File rootDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_1")
File rootDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_2_wide")
//File rootDir = new File("D:\\Work\\Numass\\data\\2017_01\\Fill_2_wide")

NumassStorage storage = NumassStorage.buildLocalNumassRoot(rootDir, true);

Collection<NMPoint> data = NumassDataUtils.joinSpectra(
        StorageUtils.loaderStream(storage)
                .filter { it.key.matches("set_.{1,3}") }
                .map {
            println "loading ${it.key}"
            it.value
        }.map { (NumassData) it }
)

data = NumassDataUtils.substractReferencePoint(data, 18600d);

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

def printPoint(Iterable<NMPoint> data, List us, int binning = 20, normalize = true) {
    List<NMPoint> points = data.findAll { it.voltage in us }.sort { it.voltage }

    Map spectra = points.first().getMap(binning, normalize).collectEntries { key, value ->
        [key, [value]]
    };

    points.eachWithIndex { it, index ->
        print "\t${it.voltage}"
        it.getMap(binning, normalize).each { k, v ->
            spectra[k].add(v)
        }
    }

    println()

    spectra.each { key, value ->
        print key
        value.each {
            print "\t${it}"
        }
        println()
    }
}

println "\n# spectra\n"

//printPoint(data, [16200d, 16400d, 16800d, 17000d, 17200d, 17700d])
printPoint(data, [14000d, 14500d, 15000d, 15500d, 16500d])

println()

Table t = new UnderflowCorrection().fitAllPoints(data, 400, 600, 3100, 20);
ColumnedDataWriter.writeTable(System.out, t, "underflow parameters")

