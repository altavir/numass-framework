package hep.dataforge.data

import hep.dataforge.Named
import hep.dataforge.utils.ContextMetaFactory

/**
 * A common interface for data providers
 * Created by darksnake on 02-Feb-17.
 */
interface DataLoader<T: Any> : ContextMetaFactory<DataNode<T>>, Named {
    companion object {
        val SMART: DataLoader<Any> = SmartDataLoader()
    }
}