/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.util.Map;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 *
 * @author Alexander Nozik
 */
public class ExpressionUtils {

    public static Double evaluate(String expression, Map<String, Double> binding) {
        Binding b = new Binding(binding);
        // Add imports for script.
        ImportCustomizer importCustomizer = new ImportCustomizer();
        // import static com.mrhaki.blog.Type.*
        importCustomizer.addStaticStars("java.lang.Math");

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addCompilationCustomizers(importCustomizer); // Create shell and execute script.

        GroovyShell shell = new GroovyShell(b,configuration);
        return (Double) shell.evaluate(expression);
    }
}
