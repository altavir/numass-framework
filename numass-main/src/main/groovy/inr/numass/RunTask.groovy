package inr.numass

import hep.dataforge.workspace.FileBasedWorkspace
import hep.dataforge.workspace.Workspace

/**
 * Created by darksnake on 18-Apr-17.
 */


cfgPath = "D:\\Work\\Numass\\sterile2016_10\\workspace.groovy"

Workspace numass = FileBasedWorkspace.build(context, new File(cfgPath).toPath())

numass.scansum "fill_1"