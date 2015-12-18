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

import static java.lang.Math.max;

/**
 * инструменты для работы с энергитическим спектром (который в каналах)
 * @author Darksnake
 */
public class ESpectrumUtils {
    public static int[] substract(int[] sp1, int[] sp2) {
        return substract(sp1, sp2, 0, sp1.length);
    }    
    
    public static int[] substract(int[] sp1, int[] sp2, int from, int to) {
        assert sp1.length == sp2.length;
        assert to >= from;
        assert to <= sp1.length;
        
        int[] res = new int[sp1.length];
        for (int i = from; i < to; i++) {
            res[i] = max(0, sp1[i]-sp2[i]);
        }
        return res;
    }        
    
   
}
