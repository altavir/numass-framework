package inr.numass

import hep.dataforge.grind.GrindWorkspaceBuilder
import inr.numass.tasks.*

/**
 * Created by darksnake on 18-Apr-17.
 */


cfgPath = "D:\\Work\\Numass\\sterile2016_10\\workspace.groovy"

GrindWorkspaceBuilder numass = new GrindWorkspaceBuilder().read(new File(cfgPath)).startup {
    it.loadTask(NumassPrepareTask)
    it.loadTask(NumassTableFilterTask)
    it.loadTask(NumassFitScanTask)
    it.loadTask(NumassSubstractEmptySourceTask)
    it.loadTask(NumassFitScanSummaryTask)
    it.loadTask(NumassFitTask)
    it.loadTask(NumassFitSummaryTask)
}

numass.scansum "fill_1"