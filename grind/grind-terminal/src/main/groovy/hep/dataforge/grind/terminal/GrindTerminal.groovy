package hep.dataforge.grind.terminal

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.grind.GrindShell
import hep.dataforge.io.IOUtils
import hep.dataforge.io.output.ANSIStreamOutput
import hep.dataforge.io.output.Output
import hep.dataforge.meta.Meta
import hep.dataforge.meta.SimpleConfigurable
import hep.dataforge.workspace.FileBasedWorkspace
import org.jline.builtins.Completers
import org.jline.reader.*
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.stream.Stream

/**
 * A REPL Groovy shell with embedded DataForge features
 * Created by darksnake on 29-Aug-16.
 */

class GrindTerminal extends SimpleConfigurable {
    private static final AttributedStyle RES = AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW);
    private static final AttributedStyle PROMPT = AttributedStyle.BOLD.foreground(AttributedStyle.CYAN);
    private static final AttributedStyle DEFAULT = AttributedStyle.DEFAULT;

    private final GrindShell shell;
    private final Terminal terminal;

    final Output renderer;

    /**
     * Build default jline console based on operating system. Do not use for preview inside IDE
     * @return
     */
    static GrindTerminal system(Context context = Global.INSTANCE) {
        context.logger.debug("Starting grind terminal using system shell")
        return new GrindTerminal(context,
                TerminalBuilder.builder()
                        .name("df")
                        .system(true)
                        .encoding("UTF-8")
                        .build()
        )
    }

    static GrindTerminal dumb(Context context = Global.INSTANCE) {
        context.logger.debug("Starting grind terminal using dumb shell")
        return new GrindTerminal(context);
    }

    GrindTerminal(Context context, Terminal terminal = null) {
        //define terminal if it is not defined
        if (terminal == null) {
            terminal = new DumbTerminal(System.in, System.out);
            terminal.echo(false);
        }
        this.terminal = terminal
        context.logger.debug("Using ${terminal.class} terminal")

        //builder shell context
        if (Global.INSTANCE == context) {
            context = Global.INSTANCE.getContext("GRIND")
        }

        //create the shell
        shell = new GrindShell(context)

        renderer = new ANSIStreamOutput(context, terminal.output())

        //bind helper commands

        shell.bind("show", this.&show);

        //shell.bind("describe", this.&describe);

        shell.bind("run", this.&run);

        //binding.setProperty("man", help);
        shell.bind("help", this.&help);

        //binding workspace builder from default location
        File wsFile = new File("workspace.groovy");
        if (wsFile.exists()) {
            try {
                context.logger.info("Found 'workspace.groovy' in default location. Using it to builder workspace.")
                shell.bind("ws", FileBasedWorkspace.build(context, wsFile.toPath()));
                context.logger.info("Workspace builder bound to 'ws'")
            } catch (Exception ex) {
                context.logger.error("Failed to builder workspace from 'workspace.groovy'", ex)
            }
        }
    }

    private Completers.TreeCompleter.Node completerNode(Object obj) {
        List<Object> objs = new ArrayList()
        if (obj != null) {
            obj.class.declaredFields.findAll { !it.synthetic }
                    .collect { obj.properties.get(it.name) }.findAll { it != null }
                    .each {
                def node = completerNode(it)
                if (node != null) {
                    objs.add(node)
                }
            }
            obj.class.declaredMethods.findAll { !it.synthetic }.each { objs.add(it.name) }
        }
        if (objs.size() > 0) {
            return Completers.TreeCompleter.node(objs as Object[])
        } else {
            return null
        }
    }

    private Completer setupCompleter() {
        new Completers.TreeCompleter(shell.getBinding().list().values().collect {
            completerNode(it)
        }.findAll { it != null })
    }

    /**
     * Apply some closure to each of sub-results using shell configuration
     * @param res
     * @return
     */
    def unwrap(Object res, Closure cl = { it }) {
        if (getConfig().getBoolean("evalClosures", false) && res instanceof Closure) {
            res = (res as Closure).call()
        } else if (getConfig().getBoolean("evalData", true) && res instanceof Data) {
            res = (res as Data).get();
        } else if (res instanceof DataNode) {
            def node = res as DataNode
            node.nodeGoal().run()// start computation of the whole node
            node.dataStream().forEach { unwrap(it, cl) };
        }

        if (getConfig().getBoolean("unwrap", true)) {
            if (res instanceof Collection) {
                (res as Collection).forEach { unwrap(it, cl) }
            } else if (res instanceof Stream) {
                (res as Stream).forEach { unwrap(it, cl) }
            }
        }
        cl.call(res);
    }

    def show(Object obj) {
        renderer.render(obj, Meta.empty())
        return null;
    }

    def run(Object obj) {
        Path scriptPath;
        if (obj instanceof File) {
            scriptPath = (obj as File).toPath();
        } else if (obj instanceof Path) {
            scriptPath = obj as Path
        } else {
            scriptPath = shell.context.getOutput().getFile(obj as String).absolutePath;
        }

        Files.newBufferedReader(scriptPath).withCloseable {
            shell.eval(it)
        }
    }

    def help() {
        this.help(null)
    }

    def help(Object obj) {
        switch (obj) {
            case null:
            case "":
                println("This is DataForge Grind terminal shell")
                println("Any Groovy statement is allowed")
                println("Current list of shell bindings:")
                shell.binding.list().each { k, v ->
                    println("\t$k")
                }

                println("In order to display state of object and show help type `help <object>`");
                break;
            case "show":
                println("Show given object in its visual representation")
                break;
            case "describe":
                println("Show meta description for the object")
                break;
            case "run":
                println("Run given script")
                break;

            default:
                describe(obj);
        }
    }

    Terminal getTerminal() {
        return terminal;
    }

    GrindShell getShell() {
        return shell
    }

    def println(String str) {
        def writer = getTerminal().writer()
        writer.println(str)
        writer.flush()
    }

    def print(String str) {
        getTerminal().writer().with {
            print(str)
        }
    }

    private def eval(String expression) {
        def start = System.currentTimeMillis()
        def res = unwrap(shell.eval(expression))
        def now = System.currentTimeMillis()
        if (getConfig().getBoolean("benchmark", true)) {
            Duration duration = Duration.ofMillis(now - start);
            shell.context.logger.debug("Expression $expression evaluated in $duration")
        }
        return res;
    }

    /**
     * Start the terminal
     * @return
     */
    def launch() {
        LineReader reader = LineReaderBuilder.builder()
                .terminal(getTerminal())
        //.completer(setupCompleter())
                .appName("DataForge Grind terminal")
                .build();
        PrintWriter writer = getTerminal().writer();

//
//        def appender = TerminalLogLayout.buildAppender(context.logger.loggerContext, terminal);
//        context.logger.addAppender(appender)

        def promptLine = new AttributedString("[${shell.context.getName()}] --> ", PROMPT).toAnsi(getTerminal());
        try {
            while (true) {
                String expression = reader.readLine(promptLine);
                if ("exit" == expression || expression == null) {
                    shell.getContext().logger.debug("Exit command received")
                    break;
                }
                try {
                    def res = eval(expression);
                    if (res != null) {
                        String str = res.toString();

//                        //abbreviating the result
//                        //TODO improve string abbreviation
//                        if (str.size() > 50) {
//                            str = str[0..50] + "..."
//                        }

                        def resStr = new AttributedStringBuilder()
                                .style(RES)
                                .append("\tres = ")
                                .style(DEFAULT)
                                .append(str);
                        println(resStr.toAnsi(getTerminal()))
                    }
                } catch (Exception ex) {
                    writer.print(IOUtils.ANSI_RED);
                    ex.printStackTrace(writer);
                    writer.print(IOUtils.ANSI_RESET);
                }
            }
        } catch (UserInterruptException ignored) {
            writer.println("Interrupted by user")
        } catch (EndOfFileException ignored) {
            writer.println("Terminated by user")
        } finally {
            shell.getContext().logger.info("Closing terminal")
            getTerminal().close()
            shell.getContext().logger.debug("Terminal closed")
        }

    }

    /**
     * Start using provided closure as initializing script
     * @param closure
     */
    def launch(@DelegatesTo(GrindShell) Closure closure) {
        this.shell.with(closure)
        launch()
    }


}
