/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import inr.numass.storage.NMFile
import inr.numass.storage.NMPoint
import inr.numass.storage.NumassData
import inr.numass.storage.NumassDataLoader
import inr.numass.utils.UnderflowCorrection

import hep.dataforge.meta.Meta
import hep.dataforge.tables.Table
import inr.numass.actions.PileupSimulationAction
import hep.dataforge.grind.GrindMetaBuilder
import hep.dataforge.io.ColumnedDataWriter

File dataDir = new File("D:\\Work\\Numass\\data\\2016_04\\T2_data\\Fill_2_2\\set_6_e26d123e54010000")
if(!dataDir.exists()){
    println "dataDir directory does not exist"
}

//println config
NumassData data = NumassDataLoader.fromLocalDir(null, dataDir)
Table t = new UnderflowCorrection().fitAllPoints(data, 500, 800, 20);
ColumnedDataWriter.writeDataSet(System.out, t, "underflow parameters")

