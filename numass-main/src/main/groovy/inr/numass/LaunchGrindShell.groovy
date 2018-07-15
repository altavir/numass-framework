package inr.numass

import hep.dataforge.context.Global
import hep.dataforge.grind.terminal.GrindTerminal
import hep.dataforge.grind.workspace.GrindWorkspace
import hep.dataforge.workspace.FileBasedWorkspace
import hep.dataforge.workspace.Workspace

/**
 * Created by darksnake on 29-Aug-16.
 */


def cli = new CliBuilder()
cli.c(longOpt: "config", args: 1, "The name of configuration file")
println cli.usage

def cfgPath = cli.parse(args).c;
println "Loading config file from $cfgPath"
println "Starting Grind shell"


try {
    GrindTerminal.system().launch {
        if (cfgPath) {
            Workspace numass = FileBasedWorkspace.build(context, new File(cfgPath as String).toPath())
            bind("numass", new GrindWorkspace(numass))
        } else {
            println "No configuration path. Provide path via --config option"
        }
    }
} catch (Exception ex) {
    ex.printStackTrace();
} finally {
    Global.INSTANCE.terminate();
}
