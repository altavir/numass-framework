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
package inr.numass.models;

import hep.dataforge.data.DataPoint;
import hep.dataforge.data.DataSet;
import hep.dataforge.io.IOUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author Darksnake
 */
public class FSS{
    private final ArrayList<Pair<Double,Double>> points;
    private double norm;

    public FSS(File FSSFile) {
        try {

            DataSet data = IOUtils.readColumnedData(FSSFile,"E","P");
            this.points = new ArrayList<>();
            norm = 0;
            for (DataPoint dp : data) {
                Double E = dp.getValue("E").doubleValue();
                Double P = dp.getValue("P").doubleValue();
                this.points.add(new Pair<>(E,P));
                norm += P;
            }
            if(points.isEmpty()) {
                throw new Error("Error reading FSS FILE. No points.");
            }
        } catch (FileNotFoundException ex) {
            throw new Error("Error reading FSS FILE. File not found.");
        }
    }

    
    
    
    double getE(int n){
        return this.points.get(n).getFirst();
    }
    
    double getP(int n){
        return this.points.get(n).getSecond() / norm;
    }
    
    boolean isEmpty(){
        return points.isEmpty();
    }
    
    int size(){
        return points.size();
        
    }
}
