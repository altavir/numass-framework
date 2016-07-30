/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass;

import hep.dataforge.context.Context;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Alexander Nozik
 */
public class MainTest {
    
    public MainTest() {
    }

    /**
     * Test of main method, of class Main.
     */
    @Test
    public void testMain() throws Exception {
        System.out.println("main");
        String[] args = null;
        Main.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of run method, of class Main.
     */
    @Test
    public void testRun() throws Exception {
        System.out.println("run");
        Context context = null;
        String[] args = null;
        Main.run(context, args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of applyCLItoContext method, of class Main.
     */
    @Test
    public void testApplyCLItoContext() throws Exception {
        System.out.println("applyCLItoContext");
        CommandLine line = null;
        Context context = null;
        Main.applyCLItoContext(line, context);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
