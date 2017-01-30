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

//File dataDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_1\\set_28")
//File dataDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_2_wide\\set_31")

File rootDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_2_wide")

NumassStorage storage = NumassStorage.buildLocalNumassRoot(rootDir, true);

Iterable<NMPoint> data = NumassDataUtils.sumSpectra(
        StorageUtils.loaderStream(storage).map { it.value }.filter { it.name.matches("set_.{2,3}") }
)

//if(!dataDir.exists()){
//    println "dataDir directory does not exist"
//}
//NumassData data = NumassDataLoader.fromLocalDir(null, dataDir)
////NumassData data = NMFile.readFile(new File("D:\\Work\\Numass\\sterilie2013-2014\\dat\\2013\\SCAN06.DAT" ))

Table t = new UnderflowCorrection().fitAllPoints(data, 400, 650, 3100, 20);
ColumnedDataWriter.writeDataSet(System.out, t, "underflow parameters")

