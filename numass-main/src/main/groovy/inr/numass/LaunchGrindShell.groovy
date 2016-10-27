package inr.numass

import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.GrindWorkspaceBuilder

/**
 * Created by darksnake on 29-Aug-16.
 */


def cli = new CliBuilder()
cli.c(longOpt: "config", args: 1, "The name of configuration file")
println cli.usage

String cfgPath = cli.parse(args).c;
println "Loading config file from $cfgPath"
new GrindShell().start {
    context.pluginManager().loadPlugin("plots-jfc")
    GrindWorkspaceBuilder numass = new GrindWorkspaceBuilder()
            .withSpec(NumassWorkspaceSpec)
            .from(new File(cfgPath))
    bind("numass", numass)
}