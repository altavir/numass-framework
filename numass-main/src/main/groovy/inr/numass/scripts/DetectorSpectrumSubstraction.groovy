/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.tables.ValueMap

NumassData.metaClass.findPoint{double u ->
    delegate.getNMPoints().getWork { it.getVoltage() == u }.getMap(20, true)
}

Map<Double, Double> dif(NumassData data1, NumassData data2, double uset){
    Map<Double, Double> spectrum1 = data1.findPoint(uset);
    Map<Double, Double> spectrum2 = data2.findPoint(uset);

    Map<Double, Double> dif = new LinkedHashMap<>();

    spectrum1.each{ key, value -> dif.put(key, Math.max(spectrum1.get(key)-spectrum2.get(key), 0d))}
    return dif;
}

def buildSet(NumassData data1, NumassData data2, double... points){
    TableFormatBuilder builder  = new TableFormatBuilder().addNumber("channel");
    List<ValueMap> pointList = new ArrayList<>();
    
    for(double point: points){
        builder.addNumber(Double.toString(point));
        Map<Double, Double> dif  = dif(data1, data2, point);
        if(pointList.isEmpty()){
            for(Double channel : dif.keySet()){
                ValueMap p = new ValueMap();
                p.putValue("channel",channel);
                pointList.add(p);
            }
        }
        for(ValueMap mp:pointList){
            double channel = mp.getValue("channel").doubleValue();
            mp.putValue(Double.toString(point), dif.get(channel));
        }
    }
    
    ListTable set = new ListTable(pointList,builder.build());
}



NumassData data1 = NMFile.readFile(new File("D:\\Work\\Numass\\transmission 2013\\STABIL04.DAT"));
NumassData data2 = NMFile.readFile(new File("D:\\Work\\Numass\\transmission 2013\\DARK04.DAT"));

double[] points = [14500,15000,15500,16000,18100,18200,18300]
 
ColumnedDataWriter.writeTable(System.out, buildSet(data1,data2,points), "Detector spectrum substraction");




