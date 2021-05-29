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
package hep.dataforge.control.measurements;

import hep.dataforge.exceptions.MeasurementException;

import java.time.Instant;

/**
 * A listener for device measurements
 *
 * @author Alexander Nozik
 */
@Deprecated
public interface MeasurementListener {

    /**
     * Measurement started. Ignored by default
     * @param measurement 
     */
    default void onMeasurementStarted(Measurement<?> measurement){
        
    }

    /**
     * Measurement stopped. Ignored by default
     * @param measurement 
     */
    default void onMeasurementFinished(Measurement<?> measurement){
        
    }

    /**
     * Measurement result obtained
     * @param measurement
     * @param result 
     */
    void onMeasurementResult(Measurement<?> measurement, Object result, Instant time);

    /**
     * Measurement failed with exception
     * @param measurement
     * @param exception 
     */
    void onMeasurementFailed(Measurement<?> measurement, Throwable exception);
    
    /**
     * Measurement failed with message
     * @param measurement
     * @param message 
     */
    default void onMeasurementFailed(Measurement<?> measurement, String message){
        onMeasurementFailed(measurement, new MeasurementException(message));
    }

    /**
     * Measurement progress updated. Ignored by default
     * @param measurement
     * @param progress 
     */
    default void onMeasurementProgress(Measurement<?> measurement, double progress) {

    }

    /**
     * Measurement progress message updated. Ignored by default
     * @param measurement
     * @param message 
     */
    default void onMeasurementProgress(Measurement<?> measurement, String message) {

    }

}
