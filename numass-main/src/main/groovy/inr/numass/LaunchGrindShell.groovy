package inr.numass

import hep.dataforge.grind.GrindShell

/**
 * Created by darksnake on 29-Aug-16.
 */

GrindShell shell = new GrindShell()
shell.launcher.withSpec(NumassWorkspaceSpec)
shell.launcher.from(new File("D:\\Work\\Numass\\sterile2016\\workspace.groovy"))
shell.start()
