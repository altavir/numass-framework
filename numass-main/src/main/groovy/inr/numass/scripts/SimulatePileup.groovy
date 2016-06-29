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
import hep.dataforge.meta.Meta
import inr.numass.actions.PileupSimulationAction
import hep.dataforge.grind.GrindMetaBuilder

File dataDir = new File("D:\\Work\\Numass\\data\\2016_04\\T2_data\\Fill_2_2\\set_6_e26d123e54010000")
if(!dataDir.exists()){
    println "dataDir directory does not exist"
}

Meta config = new GrindMetaBuilder().config(lowerChannel: 400, upperChannel: 1700)
//println config
NumassData data = NumassDataLoader.fromLocalDir(null, dataDir)
Map<String, NumassData> res = new PileupSimulationAction().simpleRun(data,config)

def keys = res.keySet();

//print spectra for selected point
double u = 15000d;

List<Map> points = res.collect{key, value -> value.getByUset(u).getMapWithBinning(20, false)}

println "\n Spectrum example for U = ${u}\n"

print "channel\t"
println keys.join("\t")

points.first().keySet().each{
    print "${it}\t"
    println points.collect{map-> map[it]}.join("\t")
}


//printing count rate in window
print "U\tLength\t"
print keys.collect{it+"[total]"}.join("\t") + "\t"
println keys.join("\t")

for(int i = 0; i < data.getNMPoints().size();i++){
    print "${data.getNMPoints().get(i).getUset()}\t"
    print "${data.getNMPoints().get(i).getLength()}\t"
    print keys.collect{res[it].getNMPoints().get(i).getEventsCount()}.join("\t") + "\t"
    println keys.collect{res[it].getNMPoints().get(i).getCountInWindow(500,1700)}.join("\t")
}
