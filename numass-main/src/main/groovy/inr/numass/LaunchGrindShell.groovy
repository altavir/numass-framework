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
//println "Starting numass plugin in GLOBAL"
//Global.instance().pluginManager().loadPlugin("inr.numass:numass")
println "Starting Grind shell"

if(cfgPath) {
    try {
        GrindTerminal.dumb().launch {
            GrindWorkspaceBuilder numass = new GrindWorkspaceBuilder()
                    .withSpec(NumassWorkspaceSpec)
                    .from(new File(cfgPath))
            shell.bind("numass", numass)
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    } finally {
        Global.terminate();
    }
} else{
    println "No configuration path. Provide path via --config option"
}