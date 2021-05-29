package hep.dataforge.grind

import groovy.transform.CompileStatic
import hep.dataforge.context.Global
import hep.dataforge.grind.extensions.ExtensionInitializer
import hep.dataforge.grind.workspace.WorkspaceSpec
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.workspace.Workspace
import org.codehaus.groovy.control.CompilerConfiguration
import org.slf4j.LoggerFactory

/**
 * Created by darksnake on 04-Aug-16.
 */
@CompileStatic
class Grind {

    static {
        LoggerFactory.getLogger("GRIND").debug("Initializing static GRIND extensions")
        ExtensionInitializer.initAll()
    }

    /**
     * Build a fully defined node with given node name, root node values and delegating closure
     * @param nodeName
     * @param values
     * @param cl
     * @return
     */
    static MetaBuilder buildMeta(String nodeName, Map values = [:], @DelegatesTo(GrindMetaBuilder) Closure cl = null) {
        MetaBuilder builder
        if (cl != null) {
            def metaSpec = new GrindMetaBuilder()
            def metaExec = cl.rehydrate(metaSpec, null, null);
            metaExec.resolveStrategy = Closure.DELEGATE_ONLY;
            builder = metaSpec.invokeMethod(nodeName, metaExec) as MetaBuilder
        } else {
            builder = new MetaBuilder();
        }

        if (!nodeName.isEmpty()) {
            builder.rename(nodeName);
        }

        builder.update(values)

        return builder
    }

    /**
     * Build anonymous meta node using {@code GrindMetaBuilder} and root node values
     * @param values
     * @param cl
     * @return
     */
    static MetaBuilder buildMeta(Map values, @DelegatesTo(GrindMetaBuilder) Closure cl = null) {
        return buildMeta("", values, cl);
    }

    /**
     * Build anonymous meta node using {@code GrindMetaBuilder}
     * @param cl
     * @return
     */
    static MetaBuilder buildMeta(@DelegatesTo(GrindMetaBuilder) Closure cl) {
        return buildMeta("", [:], cl);
    }

    static MetaBuilder buildMeta(String nodeName, @DelegatesTo(GrindMetaBuilder) Closure cl) {
        return buildMeta(nodeName, [:], cl);
    }

    /**
     * Parse meta from a string
     * @param input
     * @return
     */
    static MetaBuilder parseMeta(String input) {
        if (input.contains("(") || input.contains("{")) {
            def compilerConfiguration = new CompilerConfiguration()
            compilerConfiguration.scriptBaseClass = DelegatingScript.class.name
            def shell = new GroovyShell(Grind.class.classLoader, compilerConfiguration)
            DelegatingScript script = shell.parse(input) as DelegatingScript;
            GrindMetaBuilder builder = new GrindMetaBuilder();
            script.setDelegate(builder)
            return script.run() as MetaBuilder
        } else {
            return new MetaBuilder(input);
        }
    }

//    static Workspace buildWorkspace(File file, Class spec) {
//        return new GrindWorkspaceBuilder().read(file).withSpec(spec).builder();
//    }

//    /**
//     * A universal grind meta builder. Using reflections to determine arguments.
//     * @param args
//     * @return
//     */
//    static MetaBuilder buildMeta(Object... args) {
//        if (args.size() == 0) {
//            return new MetaBuilder("");
//        } else if (args.size() == 1 && args[0] instanceof String) {
//            return parseMeta(args[0] as String);
//        } else {
//            String nodeName = args[0] instanceof String ? args[0] : "";
//            Map values;
//            if (args[0] instanceof Map) {
//                values = args[0] as Map;
//            } else if (args.size() > 1 && args[1] instanceof Map) {
//                values = args[1] as Map;
//            } else {
//                values = [:];
//            }
//            Closure closure;
//            if (args[0] instanceof Closure) {
//                closure = args[0] as Closure;
//            } else if (args.size() > 1 && args[1] instanceof Closure) {
//                closure = args[1] as Closure;
//            } else if (args.size() > 2 && args[2] instanceof Closure) {
//                closure = args[2] as Closure;
//            } else {
//                closure = {};
//            }
//
//            return buildMeta(values, nodeName, closure);
//        }
//    }

//    static Workspace buildWorkspace(File file) {
//        return FileBasedWorkspace.Companion.build(file.toPath());
//    }
//
//    static Workspace buildWorkspace(String file) {
//        return FileBasedWorkspace.Companion.build(Paths.get(file));
//    }

    static Workspace buildWorkspace(@DelegatesTo(value = WorkspaceSpec, strategy = Closure.DELEGATE_ONLY) Closure cl) {
        WorkspaceSpec spec = new WorkspaceSpec(Global.INSTANCE);
        def script = cl.rehydrate(spec, null, null);
        script.setResolveStrategy(Closure.DELEGATE_ONLY)
        script.call()
        return spec.builder.build();
    }

//    /**
//     * Build MetaMorph using convenient meta builder
//     * @param type
//     * @param args
//     * @return
//     */
//    static <T extends MetaMorph> T morph(Class<T> type,
//                                         Map values = [:],
//                                         String nodeName = "",
//                                         @DelegatesTo(GrindMetaBuilder) Closure cl = null) {
//        MetaMorph.Companion.morph(type, buildMeta(nodeName, values, cl))
//    }

//    /**
//     * Build a simple pipe action
//     * @param cl
//     * @return
//     */
//    static <T, R> Action<T, R> pipe(Map params = Collections.emptyMap(), Closure<R> cl) {
//        return GrindPipe.build(params, cl)
//    }
//
//    static <T, R> Action<T, R> join(Map params = Collections.emptyMap(), Closure<R> cl) {
//        return GrindPipe.build(params, cl)
//    }
}
