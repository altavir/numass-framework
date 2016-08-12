package inr.numass.tasks;

import hep.dataforge.grind.JavaGrindLauncher;

import java.io.File;

/**
 * Created by darksnake on 12-Aug-16.
 */
public class GrindCaller {

    public static void main(String[] args) throws Exception {
        JavaGrindLauncher.buildWorkspace(new File("D:\\Work\\Numass\\sterile2016\\workspace.groovy")).runTask("numass.prepare", "fill_2").computeAll();
    }

}
