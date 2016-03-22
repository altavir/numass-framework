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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

/**
 *
 * @author Darksnake
 */
public class NumassPawReader {

    public RawNMFile readPaw(File file, String name) throws FileNotFoundException{
        Locale.setDefault(Locale.US);
        FileInputStream bs = new FileInputStream(file);
        return readPaw(new Scanner(bs), name);
    }
    
    public RawNMFile readPaw(String filePath) throws FileNotFoundException{
        return readPaw(new File(filePath), filePath);
    }
    

    private RawNMFile readPaw(Scanner s, String fileName) {
        RawNMFile result = new RawNMFile(fileName);

        while (s.hasNext()) {
            long eventNum = s.nextLong();
            double time = s.nextDouble();
            short chanel = s.nextShort();
            short timeTotal = s.nextShort();
            double U = s.nextDouble();

//            NumassEvent event = new NumassEvent(chanel, time);
            result.putEvent(U, chanel, time);
        }
        return result;

    }
}
