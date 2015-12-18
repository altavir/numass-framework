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
package inr.numass.control.magnet;

/**
 *
 * @author Alexander Nozik
 */
public interface MagnetStateListener {

    void acceptStatus(String name, MagnetStatus state);

    void acceptNextI(String name, double nextI);

    void acceptMeasuredI(String name, double measuredI);
    
    default void displayState(String state){
        
    }

    default void error(String name,  String errorMessage, Throwable throwable){
        throw new RuntimeException(errorMessage, throwable);
    }
    
    default void monitorTaskStateChanged(String name,  boolean monitorTaskRunning) {

    }

    default void updateTaskStateChanged(String name,  boolean updateTaskRunning) {

    }
    
    default void outputModeChanged(String name,  boolean out) {

    }   
    
    default void addressChanged(String name, int address) {

    }        

}
