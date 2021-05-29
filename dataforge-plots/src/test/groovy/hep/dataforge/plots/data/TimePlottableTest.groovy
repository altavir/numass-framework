/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hep.dataforge.plots.data

import hep.dataforge.values.Value
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 *
 * @author darksnake
 */
class TimePlottableTest extends Specification {

    def "test maxItems"(){
        when:
        TimePlot pl = new TimePlot("test",null);
        pl.setMaxItems(50);
        Instant time = DateTimeUtils.now();
        for(int i = 0; i<60; i++){
            time = time.plusMillis(10);
            pl.put(time,Value.of(time.toEpochMilli()));
        }
        then:
        pl.size() == 50;
    }
    
    def "test maxAge"(){
        when:
        TimePlot pl = new TimePlot("test",null);
        pl.setMaxAge(Duration.ofMillis(500));
        Instant time = DateTimeUtils.now();
        for(int i = 0; i<60; i++){
            time = time.plusMillis(10);
            pl.put(time,Value.of(time.toEpochMilli()));
        }
        then:
        pl.size() == 52;
    }    
	
}

