package inr.numass.scripts.workspace

import hep.dataforge.workspace.FileBasedWorkspace

import java.nio.file.Paths

/**
 * Created by darksnake on 11-Aug-16.
 */

FileBasedWorkspace.build(Paths.get("D:/Work/Numass/sterile2016/workspace.groovy")).runTask("numass.prepare", "fill_2").computeAll()

