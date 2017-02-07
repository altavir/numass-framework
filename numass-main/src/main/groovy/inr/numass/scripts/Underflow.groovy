/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.storage.commons.StorageUtils
import hep.dataforge.tables.Table
import inr.numass.storage.NMPoint
import inr.numass.storage.NumassDataUtils
import inr.numass.storage.NumassStorage
import inr.numass.utils.UnderflowCorrection

File rootDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_1")

NumassStorage storage = NumassStorage.buildLocalNumassRoot(rootDir, true);

Collection<NMPoint> data = NumassDataUtils.joinSpectra(
        StorageUtils.loaderStream(storage).filter { it.key.matches("set_.{2,3}") }.map {
            println "loading ${it.key}"
            it.value
        }
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
//    NMPoint ref = emptySpectra.find { it.uset == point.uset }
//    if (ref) {
//        println "substracting tritium background for point ${point.uset}"
//        NumassDataUtils.substractPoint(point, ref)
//    } else {
//        println "point ${point.uset} skipped"
//        point
//    }
//}

def printPoint(Iterable<NMPoint> data, List us, int binning = 20, normalize = false) {
    List<NMPoint> points = data.findAll { it.uset in us }.sort { it.uset }

    Map spectra = points.first().getMapWithBinning(binning, normalize).collectEntries { key, value ->
        [key, [value]]
    };

    points.eachWithIndex { it, index ->
        if (index > 0) {
            print "\t${it.uset}"
            it.getMapWithBinning(binning, normalize).each { k, v ->
                spectra[k].add(v)
            }
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

printPoint(data, [16200d, 16400d, 16800d, 17000d, 17200d])

println()

Table t = new UnderflowCorrection().fitAllPoints(data, 400, 700, 3100, 20);
ColumnedDataWriter.writeDataSet(System.out, t, "underflow parameters")

