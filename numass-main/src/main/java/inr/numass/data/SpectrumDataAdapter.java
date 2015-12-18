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
package inr.numass.data;

import hep.dataforge.meta.Meta;
import hep.dataforge.data.DataPoint;
import hep.dataforge.data.MapDataPoint;
import hep.dataforge.data.XYDataAdapter;
import hep.dataforge.exceptions.DataFormatException;
import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.names.Names;
import hep.dataforge.values.Value;

/**
 *
 * @author Darksnake
 */
public class SpectrumDataAdapter extends XYDataAdapter {

    private static final String ANNOTATION_TIMENAME = "timeName";

    private String timeName = "time";

    public SpectrumDataAdapter() {
    }

    public SpectrumDataAdapter(Meta aliasAnnotation) {
        super(aliasAnnotation);
        this.timeName = aliasAnnotation.getString(ANNOTATION_TIMENAME, "X");
    }

    public SpectrumDataAdapter(String xName, String yName, String yErrName, String timeTime) {
        super(xName, yName, yErrName);
        this.timeName = timeTime;
    }

    public SpectrumDataAdapter(String xName, String yName, String timeTime) {
        super(xName, yName);
        this.timeName = timeTime;
    }

    public double getTime(DataPoint point) {
        if (point.names().contains(timeName)) {
            return point.getDouble(timeName);
        } else {
            return 1d;
        }
    }
    
    public DataPoint buildSpectrumDataPoint(double x, long count, double t) {
        return new MapDataPoint(new String[]{xName,yName,timeName}, x,count,t);
    }

    public DataPoint buildSpectrumDataPoint(double x, long count, double countErr, double t) {
        return new MapDataPoint(new String[]{xName,yName,yErrName,timeName}, x,count,countErr,t);
    }      

    @Override
    public Meta buildAnnotation() {
        Meta res = super.buildAnnotation();
        res.getBuilder().putValue(ANNOTATION_TIMENAME, timeName);
        return res;
    }

    @Override
    public Names getNames() {
        return Names.of(xName,yName);
    }

    @Override
    public boolean providesYError(DataPoint point) {
        return true;
    }
    
    

    @Override
    public Value getYerr(DataPoint point) throws NameNotFoundException {
        if (point.names().contains(yErrName)) {
            return Value.of(super.getYerr(point).doubleValue()/getTime(point));
        } else{
            double y = super.getY(point).doubleValue();
            if(y<=0) throw new DataFormatException();
            else {
                return Value.of(Math.sqrt(y)/getTime(point));
            }
        }
    }
    
    public long getCount(DataPoint point){
        return point.getValue(yName).numberValue().longValue();
    }

    @Override
    public Value getY(DataPoint point) {
        return Value.of(super.getY(point).doubleValue() / getTime(point));
    }

}
