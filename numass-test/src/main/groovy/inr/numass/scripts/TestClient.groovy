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
package inr.numass.scripts

import hep.dataforge.storage.commons.StorageManager

new StorageManager().startGlobal();


println "Starting Numass test client..."
    
String line = "";

BufferedReader br = new BufferedReader(new InputStreamReader(System.in))
while(line == null || !line.startsWith("exit")){
    //    print ">"
    line = br.readLine();
    if(line!= null && !line.startsWith("exit")){
        Cli.runComand("127.0.0.1", 8335, line.split(" "));
    }
}


