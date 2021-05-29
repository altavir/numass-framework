package hep.dataforge.grind.workspace

import groovy.transform.CompileStatic
import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.workspace.Workspace
import hep.dataforge.workspace.WorkspaceParser
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import java.nio.file.Files
import java.nio.file.Path

@CompileStatic
class GroovyWorkspaceParser implements WorkspaceParser {

    @Override
    List<String> listExtensions() {
        return [".groovy", ".grind"];
    }



    @Override
    Workspace.Builder parse(Context parentContext = Global.INSTANCE, Reader reader) {
//        String scriptText = new String(reader.Files.readAllBytes(path), IOUtils.UTF8_CHARSET);

        def compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.scriptBaseClass = DelegatingScript.class.name;
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStaticStars(
                "java.lang.Math",
                "hep.dataforge.grind.Grind",
                "hep.dataforge.grind.workspace.DefaultTaskLib"
        )
        compilerConfiguration.addCompilationCustomizers(importCustomizer)

        def shell = new GroovyShell(this.class.classLoader, new Binding(), compilerConfiguration)
        DelegatingScript script = shell.parse(reader) as DelegatingScript;
        WorkspaceSpec spec = new WorkspaceSpec(parentContext)
        script.setDelegate(spec);
        script.run()
        return spec.getBuilder();
    }


    Workspace.Builder parse(Context context, Path path){
        return parse(context, Files.newBufferedReader(path))
    }
}
