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

import hep.dataforge.storage.filestorage.FileStorage
import inr.numass.server.NumassServer

String path = "D:\\temp\\test\\numass-server\\"

FileStorage storage = FileStorage.in(new File(path), null);

println "Starting test numass listener in "+path

NumassServer listener = new NumassServer(storage, null);

listener.open()

String stopLine = "";

BufferedReader br = new BufferedReader(new InputStreamReader(System.in))
while(stopLine == null || !stopLine.startsWith("exit")){
//    print ">"
    stopLine = br.readLine();
}

listener.close()

println "Stopping test numass listener"
