package inr.numass.workspace;

import hep.dataforge.context.GlobalContext;
import hep.dataforge.data.DataNode;
import hep.dataforge.grind.JavaGrindLauncher;
import hep.dataforge.meta.Meta;
import hep.dataforge.workspace.Workspace;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by darksnake on 12-Aug-16.
 */
public class NumassJavaGrindLauncher {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("c", "config", true, "Configuration file for workspace");
        options.addOption("o", "overlay", false, "If meta with the same name as parameter exist in the workspace, use it as lower layer");

        CommandLine cli = new DefaultParser().parse(options, args);

        String cfgPath = cli.getOptionValue('c', "workspace.groovy");

        List<String> arglist = cli.getArgList();

        Workspace workspace = JavaGrindLauncher.buildWorkspace(new File(cfgPath), "inr.numass.NumassWorkspaceSpec");

        if (arglist.size() == 0) {
            new HelpFormatter().printHelp("launcher [opts] <task name> <meta name / meta>", options);
            return;
        } else if (arglist.size() == 1) {
            String taskName = arglist.get(0);
            workspace.runTask(taskName, taskName);
        } else {
            String taskName = arglist.get(0);
            String theRest = arglist.stream().skip(1).collect(Collectors.joining());
            DataNode node;
            if (theRest.contains("{") || theRest.contains("(")) {
                Meta meta = JavaGrindLauncher.buildMeta(theRest);
                boolean overlay = cli.hasOption("o");
                node = workspace.runTask(taskName, meta, overlay);
            } else {
                node = workspace.runTask(taskName, theRest);
            }

            node.computeAll();
            GlobalContext.instance().close();
        }
//
//        JavaGrindLauncher.buildWorkspace(new File(cfgPath), NumassWorkspaceSpec.class)
//                .runTask("numass.fitsum", "sum").computeAll();
    }

}
