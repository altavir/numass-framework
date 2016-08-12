package inr.numass.scripts.workspace

import hep.dataforge.grind.GrindLauncher

/**
 * Created by darksnake on 11-Aug-16.
 */


new GrindLauncher().from {
    new File("D:\\Work\\Numass\\sterile2016\\workspace.groovy")
}.runTask("numass.prepare", "fill_2").computeAll()

