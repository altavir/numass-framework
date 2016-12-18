package inr.numass.scripts.workspace

import hep.dataforge.grind.GrindWorkspaceBuilder

/**
 * Created by darksnake on 11-Aug-16.
 */


new GrindWorkspaceBuilder().read {
    new File("D:\\Work\\Numass\\sterile2016\\workspace.groovy")
}.runTask("numass.prepare", "fill_2").computeAll()

