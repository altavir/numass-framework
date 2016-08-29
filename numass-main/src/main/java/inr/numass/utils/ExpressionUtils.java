/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import hep.dataforge.utils.CommonUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.util.Map;

/**
 * @author Alexander Nozik
 */
public class ExpressionUtils {
    private static Map<String, Script> cache = CommonUtils.getLRUCache(100);
    private static GroovyShell shell;

    static {
        // Add imports for script.
        ImportCustomizer importCustomizer = new ImportCustomizer();
        // import static com.mrhaki.blog.Type.*
        importCustomizer.addStaticStars("java.lang.Math");

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addCompilationCustomizers(importCustomizer); // Create shell and execute script.
        shell = new GroovyShell(configuration);
    }

    private static Script getScript(String expression) {
        return cache.computeIfAbsent(expression, expr -> shell.parse(expr));
    }


    public static double evaluate(String expression, Map<String, Object> binding) {
        synchronized (cache) {
            Binding b = new Binding(binding);
            Script script = getScript(expression);
            script.setBinding(b);
            return ((Number) script.run()).doubleValue();
        }
    }
}
