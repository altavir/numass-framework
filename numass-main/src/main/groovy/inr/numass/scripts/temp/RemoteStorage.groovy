package inr.numass.scripts.temp

import hep.dataforge.context.Context
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindShell
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.commons.StorageManager

new GrindShell().eval {
    def ctx = context as Context;
    //(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).setLevel(Level.INFO)

    def storageMeta = Grind.buildMeta(type: "numass", path: "sftp://192.168.111.1/home/trdat/data/2017_11", userName: "trdat", password: "Anomaly")

    Storage storage = ctx.load("hep.dataforge:storage", StorageManager).buildStorage(storageMeta);

}