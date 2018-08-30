package inr.numass.data.storage

import hep.dataforge.context.Context
import hep.dataforge.data.DataFactory
import hep.dataforge.data.DataNodeEditor
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.storage.Storage
import hep.dataforge.storage.StorageElement
import inr.numass.data.api.NumassSet
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
        runBlocking {
            val storage = NumassDirectory.read(context,meta.getString("path")) as Storage
            storage.sequence().forEach { pair ->
                val value = pair.second
                if (value is NumassSet) {
                    builder.putStatic(pair.first.unescaped, value)
                }
            }
        }
    }
}
