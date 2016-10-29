/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.tables.Table
import inr.numass.storage.NumassData
import inr.numass.storage.NumassDataLoader
import inr.numass.utils.UnderflowCorrection

//File dataDir = new File("D:\\Work\\Numass\\data\\2016_04\\T2_data\\Fill_2_2\\set_7_b2a3433e54010000")
//File dataDir = new File("D:\\Work\\Numass\\data\\2016_04\\T2_data\\Fill_2_2\\set_6_e26d123e54010000")
File dataDir = new File("D:\\Work\\Numass\\data\\2016_10\\Fill_2_wide\\\\set_21")
if(!dataDir.exists()){
    println "dataDir directory does not exist"
}
NumassData data = NumassDataLoader.fromLocalDir(null, dataDir)
//NumassData data = NMFile.readFile(new File("D:\\Work\\Numass\\sterilie2013-2014\\dat\\2013\\SCAN06.DAT" ))

Table t = new UnderflowCorrection().fitAllPoints(data, 500, 1000, 1800, 20);
ColumnedDataWriter.writeDataSet(System.out, t, "underflow parameters")

