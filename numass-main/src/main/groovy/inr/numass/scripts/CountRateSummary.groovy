package inr.numass.scripts


import hep.dataforge.tables.Table
import hep.dataforge.workspace.FileBasedWorkspace
import hep.dataforge.workspace.Workspace

import java.nio.file.Paths

/**
 * Created by darksnake on 26-Dec-16.
 */


Workspace numass = FileBasedWorkspace.build(Paths.get("D:/Work/Numass/sterile2016_10/workspace.groovy"))

numass.runTask("prepare", "fill_1_all").forEachData(Table) {
    Table table = it.get();
    def dp18 = table.find { it["Uset"] == 18000 }
    def dp17 = table.find { it["Uset"] == 17000 }
    println "${it.name}\t${dp18["CR"]}\t${dp18["CRerr"]}\t${dp17["CR"]}\t${dp17["CRerr"]}"
}

