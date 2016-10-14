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
package inr.numass.readvac;

import hep.dataforge.utils.DateTimeUtils;

import java.time.*;

/**
 *
 * @author Darksnake
 */
public class TimeShiftTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Instant now = DateTimeUtils.now();
        System.out.println(now.toString());
        LocalDateTime ldt = LocalDateTime.now();
        System.out.println(ldt.toString());
        System.out.println(ldt.toInstant(ZoneOffset.ofHours(1)).toString());       
        ZonedDateTime zdt = ZonedDateTime.now();
        System.out.println(zdt.toString());
        System.out.println(zdt.toInstant()); 
        
        System.out.println(ZoneId.systemDefault().getRules().toString());  
       
        
    }
    
}
