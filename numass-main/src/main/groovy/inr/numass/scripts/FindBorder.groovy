/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import inr.numass.storage.NMFile
import inr.numass.storage.NumassData
import inr.numass.storage.NumassDataLoader
import hep.dataforge.meta.Meta
import inr.numass.actions.FindBorderAction
import hep.dataforge.grind.GrindMetaBuilder

File dataDir = new File("D:\\Work\\Numass\\data\\2016_04\\T2_data\\Fill_1_7\\set_2_3b127e3254010000")
if(!dataDir.exists()){
    println "dataDir directory does not exist"
}
Meta config = new GrindMetaBuilder().config(lower: 500, upper: 1600)
println config
NumassData data = NumassDataLoader.fromLocalDir(null, dataDir)
new FindBorderAction().eval(data, config)
