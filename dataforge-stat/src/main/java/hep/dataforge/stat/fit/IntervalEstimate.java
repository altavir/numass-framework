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
package hep.dataforge.stat.fit;

import hep.dataforge.names.NameSetContainer;
import hep.dataforge.values.Value;
import kotlin.Pair;

import java.io.PrintWriter;
import java.io.Serializable;

/**
 * Interface for representing special errors or fit information e.g. asymmetrical
 * errors or confidence intervals
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public interface IntervalEstimate extends Serializable, NameSetContainer {

    /**
     * <p>print.</p>
     *
     * @param out a {@link java.io.PrintWriter} object.
     */
    void print(PrintWriter out);

    /**
     * Get univariate interval estimate for given parameter with default confidence limit
     *
     * @param parName
     * @return
     */
    Pair<Value, Value> getInterval(String parName);

    default Value gerLowerBound(String parName) {
        return getInterval(parName).getFirst();
    }

    default Value gerUpperBound(String parName) {
        return getInterval(parName).getSecond();
    }

    /**
     * get the default confidence limit
     *
     * @return
     */
    double getCL();
    
}
