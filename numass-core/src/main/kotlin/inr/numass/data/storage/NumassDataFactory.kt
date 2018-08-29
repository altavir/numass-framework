package inr.numass.data.storage

import hep.dataforge.context.Context
import hep.dataforge.data.DataFactory
import hep.dataforge.data.DataNodeEditor
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.storage.Storage
import hep.dataforge.storage.StorageElement
import hep.dataforge.storage.StorageManager
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDirectory.Companion.NUMASS_DIRECTORY_TYPE
import kotlinx.coroutines.experimental.runBlocking
import kotlin.coroutines.experimental.buildSequence

/**
 * Created by darksnake on 03-Feb-17.
 */
class NumassDataFactory : DataFactory<NumassSet>(NumassSet::class.java) {

    override val name = "numass"

    /**
     * Build the sequence of name
     */
    private fun Storage.sequence(prefix: Name = Name.empty()): Sequence<Pair<Name, StorageElement>> {
        return buildSequence {
            runBlocking { getChildren() }.forEach {
                val newName = prefix + it.name
                yield(Pair(newName, it))
                if (it is Storage) {
                    yieldAll(it.sequence(newName))
                }
            }
        }

    }

    override fun fill(builder: DataNodeEditor<NumassSet>, context: Context, meta: Meta) {
        val newMeta = meta.builder.setValue("type", NUMASS_DIRECTORY_TYPE)
        runBlocking {
            val storage = context.load(StorageManager::class.java, Meta.empty()).create(newMeta) as Storage
            storage.sequence().forEach { pair ->
                val value = pair.second
                if (value is NumassSet) {
                    builder.putStatic(pair.first.unescaped, value)
                }
            }
        }
    }
}
