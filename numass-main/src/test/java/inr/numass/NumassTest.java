/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass;

import hep.dataforge.context.Context;
import hep.dataforge.maths.functions.FunctionLibrary;
import org.junit.Test;

/**
 *
 * @author Alexander Nozik
 */
public class NumassTest {
    /**
     * Test of buildContext method, of class Numass.
     */
    @Test
    public void testBuildContext() {
        Context context = Numass.buildContext();
        FunctionLibrary.Companion.buildFrom(context);
    }

}
