package inr.numass.data.storage

import hep.dataforge.context.Context
import hep.dataforge.data.DataFactory
import hep.dataforge.data.DataNodeEditor
import hep.dataforge.meta.Meta
import hep.dataforge.storage.commons.StorageManager
import hep.dataforge.storage.commons.StorageUtils
import inr.numass.data.api.NumassSet

/**
 * Created by darksnake on 03-Feb-17.
 */
class NumassDataFactory : DataFactory<NumassSet>(NumassSet::class.java) {

    override val name= "numass"


    override fun fill(builder: DataNodeEditor<NumassSet>, context: Context, meta: Meta) {
        val newMeta = meta.builder.setValue("type", "numass")
        val storage = context.load(StorageManager::class.java, Meta.empty()).buildStorage(newMeta)
        StorageUtils.loaderStream(storage).forEach { loader ->
            if (loader is NumassSet) {
                builder.putStatic(loader.fullName.toUnescaped(), loader as NumassSet)
            }
        }
    }
}
