/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.MapPoint
import hep.dataforge.tables.Table
import inr.numass.data.NumassData
import inr.numass.data.*
import javafx.stage.FileChooser


NumassData.metaClass.findPoint{double u ->
    delegate.getNMPoints().find{it.getUset() == u}.getMapWithBinning(20,true)
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
    List<MapPoint> pointList = new ArrayList<>();
    
    for(double point: points){
        builder.addNumber(Double.toString(point));
        Map<Double, Double> dif  = dif(data1, data2, point);
        if(pointList.isEmpty()){
            for(Double channel : dif.keySet()){
                MapPoint p = new MapPoint();
                p.putValue("channel",channel);
                pointList.add(p);
            }
        }
        for(MapPoint mp:pointList){
            double channel = mp.getValue("channel").doubleValue();
            mp.putValue(Double.toString(point), dif.get(channel));
        }
    }
    
    ListTable set = new ListTable(pointList,builder.build());
}



NumassData data1 = NMFile.readFile(new File("D:\\Work\\Numass\\transmission 2013\\STABIL04.DAT"));
NumassData data2 = NMFile.readFile(new File("D:\\Work\\Numass\\transmission 2013\\DARK04.DAT"));

double[] points = [14500,15000,15500,16000,18100,18200,18300]
 
ColumnedDataWriter.writeDataSet(System.out, buildSet(data1,data2,points), "Detector spectrum substraction");




