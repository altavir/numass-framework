package inr.numass.tasks;

import hep.dataforge.grind.JavaGrindLauncher;
import inr.numass.NumassWorkspaceSpec;

import java.io.File;

/**
 * Created by darksnake on 12-Aug-16.
 */
public class NumassGrindLauncher {

    public static void main(String[] args) throws Exception {
        JavaGrindLauncher.buildWorkspace(new File("D:\\Work\\Numass\\sterile2016\\workspace.groovy"), NumassWorkspaceSpec.class)
                .runTask("numass.fitsum", "fill_3").computeAll();
    }

}
