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
package inr.numass.prop.ar;

import hep.dataforge.content.NamedMetaHolder;
import hep.dataforge.points.DataPoint;
import hep.dataforge.points.ListPointSet;
import hep.dataforge.points.MapPoint;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import hep.dataforge.points.PointSet;

/**
 *
 * @author Darksnake
 */
@NodeDef(name = "temperature", info = "The temperature measurements data for this spectrum.")
@ValueDef(name = "relativeStartTime", type = "NUMBER", info = "Start time in days relative to some starting point.")
@ValueDef(name = "relativeStopTime", type = "NUMBER", info = "Stop time in days relative to some starting point.")
public class JNASpectrum extends NamedMetaHolder {

    public static String[] names = {"chanel", "count"};

    private LinkedHashMap<Double, Long> spectrum;

    public JNASpectrum(String name, Meta annotation, Map<Double, Long> spectrum) {
        super(name, annotation);
        this.spectrum = new LinkedHashMap<>(spectrum);
    }

    public JNASpectrum(String name, Meta annotation, double[] x, long[] y) {
        super(name, annotation);

        if (x.length != y.length) {
            throw new IllegalArgumentException();
        }

        this.spectrum = new LinkedHashMap<>(x.length);
        for (int i = 0; i < x.length; i++) {
            spectrum.put(x[i], y[i]);
        }
    }

    public PointSet asDataSet() {
        List<DataPoint> points = new ArrayList<>();
        for (Map.Entry<Double, Long> point : spectrum.entrySet()) {
            points.add(new MapPoint(names, point.getKey(), point.getValue()));
        }
        return new ListPointSet(getName(), meta(), points);
    }

    public Map<Double, Long> asMap() {
        return Collections.unmodifiableMap(spectrum);
    }

    public double startTime() {
        return meta().getDouble("relativeStartTime");
    }
    
    public double stopTime() {
        return meta().getDouble("relativeStopTime");
    }    

    public double length(){
        return stopTime() - startTime();
    }
    
    public boolean hasTemperature() {
        return this.meta().hasNode("temperature");
    }

    public DataPoint getTemperatures() {
        if (!hasTemperature()) {
            throw new IllegalStateException("temperature are not present");
        } else {
//            double T1 = getDouble("temperature.T1",0);
//            double T2 = getDouble("temperature.T2",0);
//            double T3 = getDouble("temperature.T3",0);
//            double T4 = getDouble("temperature.T4",0);
//            double T5 = getDouble("temperature.T5",0);
//            double T6 = getDouble("temperature.T6",0);
            MapPoint res = new MapPoint();
            Meta temps = meta().getNode("temperature");
            for (String name : temps.getValueNames()) {
                res.putValue(name, temps.getValue(name));
            }
            return res;
        }
    }
    
    public JNASpectrum mergeWith(String name, JNASpectrum sp){
        LinkedHashMap<Double, Long> points = new LinkedHashMap<>();
        for (Map.Entry<Double, Long> entry : this.spectrum.entrySet()) {
            points.put(entry.getKey(), entry.getValue() + sp.spectrum.get(entry.getKey()));
        }
        
        
        MetaBuilder temperatures = new MetaBuilder("temperature");
        
        if(this.hasTemperature() && sp.hasTemperature()){
            DataPoint temps1 = this.getTemperatures();
            DataPoint temps2 = sp.getTemperatures();
            
            for(String tName: temps1.namesAsArray()){
                temperatures.putValue(tName, (temps1.getDouble(tName)*this.length() + temps2.getDouble(tName)*sp.length())/(this.length() + sp.length()));
            }
        }
        
        MetaBuilder an = new MetaBuilder("")
                .putValue("relativeStartTime", Math.min(this.startTime(), sp.startTime()))
                .putValue("relativeStopTime", Math.max(this.stopTime(), sp.stopTime()));

        if(!temperatures.isEmpty()){
            an.putNode(temperatures);
        }
        
        return new JNASpectrum(name, an.build(), points);
    }

}
