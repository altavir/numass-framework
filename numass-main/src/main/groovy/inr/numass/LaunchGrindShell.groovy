package inr.numass

import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.GrindWorkspaceBuilder

/**
 * Created by darksnake on 29-Aug-16.
 */

new GrindShell().start {
    GrindWorkspaceBuilder numass = new GrindWorkspaceBuilder()
            .withSpec(NumassWorkspaceSpec)
            .from(new File("D:\\Work\\Numass\\sterile2016\\workspace.groovy"))
    bind("numass", numass)
}