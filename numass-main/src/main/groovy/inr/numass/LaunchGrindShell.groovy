package inr.numass

import hep.dataforge.grind.GrindLauncher
import hep.dataforge.grind.GrindShell
import hep.dataforge.workspace.Workspace

/**
 * Created by darksnake on 29-Aug-16.
 */

new GrindShell().start {
    Workspace numass = new GrindLauncher()
            .withSpec(NumassWorkspaceSpec)
            .from(new File("D:\\Work\\Numass\\sterile2016\\workspace.groovy"))
            .buildWorkspace()
    setContext(numass.getContext())
    bind("numass", numass)
}