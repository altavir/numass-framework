package inr.numass

import hep.dataforge.grind.GrindLauncher
import hep.dataforge.grind.GrindShell
import hep.dataforge.workspace.Workspace

/**
 * Created by darksnake on 29-Aug-16.
 */

GrindShell shell = new GrindShell()
Workspace numass = new GrindLauncher().withSpec(NumassWorkspaceSpec).from(new File("D:\\Work\\Numass\\sterile2016\\workspace.groovy")).buildWorkspace()
shell.bind("numass", numass)
shell.start()
