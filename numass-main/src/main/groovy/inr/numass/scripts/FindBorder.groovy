/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.grind.GrindMetaBuilder
import hep.dataforge.meta.Meta
import inr.numass.actions.FindBorderAction
import inr.numass.data.storage.NumassDataLoader

File dataDir = new File("D:\\Work\\Numass\\data\\2016_04\\T2_data\\Fill_2_2\\set_6_e26d123e54010000")
if(!dataDir.exists()){
    println "dataDir directory does not exist"
}
Meta config = new GrindMetaBuilder().config(lower: 400, upper: 1800, reference: 18500)
println config
NumassData data = NumassDataLoader.fromLocalDir(null, dataDir)
new FindBorderAction().simpleRun(data, config)
