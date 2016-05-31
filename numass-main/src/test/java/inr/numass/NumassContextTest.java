/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass;

import inr.numass.storage.SetDirectionUtility;
import org.junit.Test;

/**
 *
 * @author Alexander Nozik
 */
public class NumassContextTest {
    
    public NumassContextTest() {
    }

    /**
     * Test of close method, of class NumassContext.
     */
    @Test
    public void testClose() throws Exception {
        System.out.println("close");
        NumassContext instance = new NumassContext();
        instance.close();
        assert SetDirectionUtility.cacheFile(instance).exists();
        
    }
    
}
