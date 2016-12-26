package inr.numass.scripts

import hep.dataforge.grind.GrindWorkspaceBuilder
import hep.dataforge.tables.Table
import inr.numass.workspace.*

/**
 * Created by darksnake on 26-Dec-16.
 */


GrindWorkspaceBuilder numass = new GrindWorkspaceBuilder().read(new File("D:\\Work\\Numass\\sterile2016_10\\workspace.groovy")).startup {
    it.loadTask(NumassPrepareTask)
    it.loadTask(NumassTableFilterTask)
    it.loadTask(NumassFitScanTask)
    it.loadTask(NumassSubstractEmptySourceTask)
    it.loadTask(NumassFitScanSummaryTask)
    it.loadTask(NumassFitTask)
    it.loadTask(NumassFitSummaryTask)
}

numass.runTask("prepare", "fill_1_all").forEachDataWithType(Table) {
    Table table = it.get();
    def dp18 = table.find { it["Uset"] == 18000 }
    def dp17 = table.find { it["Uset"] == 17000 }
    println "${it.name}\t${dp18["CR"]}\t${dp18["CRerr"]}\t${dp17["CR"]}\t${dp17["CRerr"]}"
}

