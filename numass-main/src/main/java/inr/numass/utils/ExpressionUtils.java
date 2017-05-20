/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import hep.dataforge.utils.Misc;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.util.Map;

/**
 * @author Alexander Nozik
 */
public class ExpressionUtils {
    private static final Map<String, Script> cache = Misc.getLRUCache(100);
    private static final GroovyShell shell;

    static {
        // Add imports for script.
        ImportCustomizer importCustomizer = new ImportCustomizer();
        // import static com.mrhaki.blog.Type.*
        importCustomizer.addStaticStars("java.lang.Math");
        //importCustomizer.addStaticStars("org.apache.commons.math3.util.FastMath");

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addCompilationCustomizers(importCustomizer); // Create shell and execute script.
        shell = new GroovyShell(configuration);
    }

    private static Script getScript(String expression) {
        return cache.computeIfAbsent(expression, shell::parse);
    }


    public static double function(String expression, Map<String, ?> binding) {
        synchronized (cache) {
            Binding b = new Binding(binding);
            Script script = getScript(expression);
            script.setBinding(b);
            return ((Number) script.run()).doubleValue();
        }
    }

    public static boolean condition(String expression, Map<String, ?> binding){
        synchronized (cache) {
            Binding b = new Binding(binding);
            Script script = getScript(expression);
            script.setBinding(b);
            return (boolean) script.run();
        }
    }
}
