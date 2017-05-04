package inr.numass

import hep.dataforge.context.Global
import hep.dataforge.grind.GrindWorkspaceBuilder
import hep.dataforge.grind.terminal.GrindTerminal

/**
 * Created by darksnake on 29-Aug-16.
 */


def cli = new CliBuilder()
cli.c(longOpt: "config", args: 1, "The name of configuration file")
println cli.usage

String cfgPath = cli.parse(args).c;
println "Loading config file from $cfgPath"
println "Starting Grind shell"


try {
    GrindTerminal.system().launch {
        if (cfgPath) {
            GrindWorkspaceBuilder numass = new GrindWorkspaceBuilder(context).read(new File(cfgPath))
            bind("numass", numass)
        } else {
            println "No configuration path. Provide path via --config option"
        }
    }
} catch (Exception ex) {
    ex.printStackTrace();
} finally {
    Global.terminate();
}
