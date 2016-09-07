package inr.numass

import hep.dataforge.grind.GrindLauncher
import hep.dataforge.grind.GrindShell

/**
 * Created by darksnake on 29-Aug-16.
 */

new GrindShell().start {
    GrindLauncher numass = new GrindLauncher()
            .withSpec(NumassWorkspaceSpec)
            .from(new File("D:\\Work\\Numass\\sterile2016\\workspace.groovy"))
    bind("numass", numass)
}