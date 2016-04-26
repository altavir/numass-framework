/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.actions;

import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.SimplePointSource;
import hep.dataforge.values.Value;
import inr.numass.data.NMFile;
import inr.numass.data.NMPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author Darksnake
 */
public class SlicedData extends SimplePointSource {
    private static final String TNAME = "Time"; 
    //format = {U,username1,username2, ...}
    private static final String UNAME = "U";

    
    private static TableFormat prepateFormat(Map<String,Pair<Integer,Integer>> intervals){
        ArrayList<String> names = new ArrayList<>(intervals.keySet());
        names.add(0, TNAME);        
        names.add(0, UNAME);
        return TableFormat.fixedWidth(8, names);
    }

    
    
    public SlicedData(NMFile file, Map<String,Pair<Integer,Integer>> intervals, boolean normalize) {
        super(prepateFormat(intervals));
        fill(file, intervals, normalize);
    }
    
    private void fill(NMFile file, Map<String,Pair<Integer,Integer>> intervals, boolean normalize){
        for (NMPoint point : file.getNMPoints()) {
            
            //создаем основу для будущей точки
            HashMap<String,Value> map = new HashMap<>();
            
            //Кладем напряжение
            map.put(UNAME, Value.of(point.getUset()));
            double t = point.getLength();
            map.put(TNAME,  Value.of(t));
            
            for (Map.Entry<String, Pair<Integer, Integer>> entry : intervals.entrySet()) {
                String name = entry.getKey();
                Pair<Integer, Integer> pair = entry.getValue();
                int a = pair.getFirst();
                int b = pair.getSecond();
                
                int count;
                // проверяем порядок границ и переворачиваем если нужно
                if(b>a){
                    count = point.getCountInWindow(a, b);
                } else if(b<a) {
                    count = point.getCountInWindow(b, a);
                } else{
                    count = point.getCountInChanel(a);
                }
                //пихаем все в map
                if(normalize){
                    map.put(name,  Value.of(count/t));
                } else {
                    map.put(name,  Value.of(count));
                }
            }
            this.addRow(new MapPoint(map));
        }
        
    }
    
}
