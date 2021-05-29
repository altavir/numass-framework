package hep.dataforge.grind.workspace

import groovy.transform.TupleConstructor
import hep.dataforge.actions.Action
import hep.dataforge.actions.OneToOneAction
import hep.dataforge.context.Context
import hep.dataforge.description.NodeDef
import hep.dataforge.io.IOUtils
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import org.slf4j.Logger

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * A specification for system exec task
 *
 */
class ExecSpec {

    /**
     * A task input handler. By default ignores input object.
     */
    private Closure handleInput = Closure.IDENTITY;

    /**
     * Handle task output. By default returns the output as text.
     */
    private Closure handleOutput = Closure.IDENTITY;

    /**
     * Build command line
     */
    private Closure cliTransform = Closure.IDENTITY;

    String actionName = "exec";

    void input(@DelegatesTo(value = InputTransformer, strategy = Closure.DELEGATE_ONLY) Closure handleInput) {
        this.handleInput = handleInput
    }

    void output(@DelegatesTo(value = OutputTransformer, strategy = Closure.DELEGATE_ONLY) Closure handleOutput) {
        this.handleOutput = handleOutput
    }

    void cli(@DelegatesTo(value = CLITransformer, strategy = Closure.DELEGATE_ONLY) Closure cliTransform) {
        this.cliTransform = cliTransform
    }

    void name(String name) {
        this.actionName = name;
    }

    Action build() {
        return new GrindExecAction(actionName);
    }

    @TupleConstructor
    private class InputTransformer {
        final String name;
        final Object input;
        final Laminate meta

        private ByteArrayOutputStream stream;

        InputTransformer(String name, Object input, Laminate meta) {
            this.name = name
            this.input = input
            this.meta = meta
        }

        ByteArrayOutputStream getStream() {
            if (stream == null) {
                stream = new ByteArrayOutputStream();
            }
            return stream
        }

        def print(Object obj) {
            getStream().print(obj)
        }

        def println(Object obj) {
            getStream().println(obj)
        }

        def printf(String format, Object... args) {
            getStream().printf(format, args)
        }
    }

    @TupleConstructor
    private class OutputTransformer {

        /**
         * The name of the data
         */
        final String name;

        /**
         * Context for task execution
         */
        final Context context;

        /**
         * task configuration
         */
        final Laminate meta;

        final String out;
        final String err;

        OutputTransformer(Context context, String name, Laminate meta, String out, String err) {
            this.name = name
            this.context = context
//            this.process = process
            this.meta = meta
            this.out = out;
            this.err = err;
        }

        private OutputStream outputStream;

        /**
         * Create task output (not result)
         * @return
         */
        OutputStream getStream() {
            if (stream == null) {
                outputStream = context.getOutput().out(actionName, name)
            }
            return stream
        }

        /**
         * Print something to default task output
         * @param obj
         * @return
         */
        def print(Object obj) {
            getStream().print(obj)
        }

        def println(Object obj) {
            getStream().println(obj)
        }

        def printf(String format, Object... args) {
            getStream().printf(format, args)
        }

//        /**
//         * Render a markedup object into default task output
//         * @param markedup
//         * @return
//         */
//        def render(Markedup markedup) {
//            new SimpleMarkupRenderer(getStream()).render(markedup.markup())
//        }
    }

    @TupleConstructor
    private class CLITransformer {
        final Context context
        final String name
        final Meta meta


        String executable = ""
        List<String> cli = [];

        CLITransformer(Context context, String name, Meta meta) {
            this.context = context
            this.name = name
            this.meta = meta
        }

        /**
         * Apply inside parameters only if OS is windows
         * @param cl
         * @return
         */
        def windows(@DelegatesTo(CLITransformer) Closure cl) {
            if (System.properties['os.name'].toLowerCase().contains('windows')) {
                this.with(cl)
            }
        }

        /**
         * Apply inside parameters only if OS is linux
         * @param cl
         * @return
         */
        def linux(@DelegatesTo(CLITransformer) Closure cl) {
            if (System.properties['os.name'].toLowerCase().contains('linux')) {
                this.with(cl)
            }
        }

        def executable(String exec) {
            this.executable = executable
        }

        def append(String... commands) {
            cli.addAll(commands)
        }

        def argument(String key = "", Object obj) {
            String value;
            if (obj instanceof File) {
                value = obj.absoluteFile.toString();
            } else if (obj instanceof URL) {
                value = new File(obj.toURI()).absoluteFile.toString();
            } else {
                value = obj.toString()
            }

            if (key) {
                cli.add(key)
            }

            cli.add(value);
        }

        /**
         * Create
         * @return
         */
        private List<String> transform() {
            return [] +
                    meta.getString("exec", executable) +
                    cli +
                    Arrays.asList(meta.getStringArray("command", new String[0]))
        }
    }

//    @ValueDef(name = "inheritIO", type = ValueType.BOOLEAN, def = "true", info = "Define if process should inherit IO from DataForge process")
    
    @NodeDef(key = "env", info = "Environment variables as a key-value pairs")
//    @NodeDef(name = "parameter", info = "The definition for command parameter")
    private class GrindExecAction extends OneToOneAction<Object, Object> {

        GrindExecAction(String name) {
            super(name, Object, Object)
        }

        @Override
        protected Object execute(Context context, String name, Object input, Laminate meta) {
            Logger logger = getLogger(context, meta);

            try {

                StringBuilder out = new StringBuilder();
                StringBuilder err = new StringBuilder()

                ProcessBuilder builder = buildProcess(context, name, input, meta);
                logger.info("Starting process with command \"" + String.join(" ", builder.command()) + "\"");

                Process process = builder.start();
                process.consumeProcessOutput(out, err)

                //sending input into process
                ByteBuffer bytes = transformInput(name, input, meta);
                if (bytes != null && bytes.limit() > 0) {
                    logger.debug("The action input is transformed into byte array with length of " + bytes.limit());
                    process.getOutputStream().write(bytes.array());
                }

                //consume process output
                logger.debug("Handling process output");

                if (process.isAlive()) {
                    logger.debug("Starting listener for process end");
                    try {
                        if (meta.hasValue("timeout")) {
                            if (!process.waitFor(meta.getInt("timeout"), TimeUnit.MILLISECONDS)) {
                                process.destroyForcibly();
                            }
                        } else {
                            logger.info("Process finished with exit value " + process.waitFor());
                        }
                    } catch (Exception ex) {
                        logger.debug("Process failed to complete", ex);
                    }
                } else {
                    logger.info("Process finished with exit value " + process.exitValue());
                }

                return transformOutput(context, name, meta, out.toString(), err.toString());
            } catch (IOException e) {
                throw new RuntimeException("Process execution failed with error", e);
            }
        }

        ProcessBuilder buildProcess(Context context, String name, Object input, Laminate meta) {
            //setting up the process
            ProcessBuilder builder = new ProcessBuilder(getCommand(context, name, meta));

            //updating environment variables
            if (meta.hasMeta("env")) {
                MetaUtils.nodeStream(meta.getMeta("env")).forEach { envNode ->
                    builder.environment().put(envNode.getValue().getString("name", envNode.getKey()), envNode.getValue().getString("value"));
                }
            }

            // Setting working directory
            if (meta.hasValue("workDir")) {
                builder.directory(context.getOutput().getFile(meta.getString("workDir")));
            }

//            if (meta.getBoolean("inheritIO", true)) {
//                builder.inheritIO();
//            }
            return builder;
        }


        ByteBuffer transformInput(String name, Object input, Laminate meta) {
            def inputTransformer = new InputTransformer(name, input, meta);
            def handler = handleInput.rehydrate(inputTransformer, null, null);
            handler.setResolveStrategy(Closure.DELEGATE_ONLY);
            def res = handler.call();

            //If stream output is initiated, use it, otherwise, convert results
            if (inputTransformer.stream != null) {
                return ByteBuffer.wrap(inputTransformer.stream.toByteArray());
            } else if (res instanceof ByteBuffer) {
                return res;
            } else if (res != null) {
                return ByteBuffer.wrap(res.toString().getBytes(IOUtils.UTF8_CHARSET))
            } else {
                return null
            }
        }

        /**
         * Transform action output. By default use text output
         * @param context
         * @param name
         * @param meta
         * @param out
         * @param err
         * @return
         */
        Object transformOutput(Context context, String name, Laminate meta, String out, String err) {
            def outputTransformer = new OutputTransformer(context, name, meta, out, err);
            def handler = handleOutput.rehydrate(outputTransformer, null, null);
            handler.setResolveStrategy(Closure.DELEGATE_ONLY);
            return handler.call() ?: out;
        }

        List<String> getCommand(Context context, String name, Meta meta) {
            def transformer = new CLITransformer(context, name, meta);
            def handler = cliTransform.rehydrate(transformer, null, null);
            handler.setResolveStrategy(Closure.DELEGATE_ONLY);
            handler.call()
            return transformer.transform().findAll { !it.isEmpty() }
        }
    }
}
