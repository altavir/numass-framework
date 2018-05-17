package inr.numass.scripts.workspace

import hep.dataforge.actions.ActionUtils
import hep.dataforge.context.Context
import hep.dataforge.io.OutputManager
import inr.numass.NumassPlugin

/**
 * Created by darksnake on 12-Aug-16.
 */


Context context = new Context("numass");
context.loadPlugin(new NumassPlugin());
context.setValue(OutputManager.ROOT_DIRECTORY_CONTEXT_KEY, "D:\\Work\\Numass\\sterile2016");
ActionUtils.runConfig(context, "test.xml").computeAll()