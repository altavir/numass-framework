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
package hep.dataforge.io.history;


/**
 * An object that could handle and store its own report. A purpose of DataForge report
 is different from standard logging because analysis report is part of the
 result. Therfore logable objects should be used only when one needs to sore
 resulting report.
 *
 * @author Alexander Nozik
 */
public interface History {

    Chronicle getChronicle();

    default void report(String str, Object... parameters){
        getChronicle().report(str, parameters);
    }

    default void report(Record entry){
        getChronicle().report(entry);
    }
    
    default void reportError(String str, Object... parameters){
        getChronicle().reportError(str, parameters);
    }
    
}
