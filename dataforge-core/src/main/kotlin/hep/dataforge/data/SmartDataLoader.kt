package hep.dataforge.data

import hep.dataforge.context.Context
import hep.dataforge.meta.Meta


/**
 * A data loader that delegates loading to a specific loader
 */
class SmartDataLoader : DataLoader<Any> {

    override val name: String = "smart"

    override fun build(context: Context, meta: Meta): DataNode<Any> {
        return getFactory(context, meta).build(context, meta)
    }

    companion object {
        const val FACTORY_TYPE_KEY = "loader"

        @Suppress("UNCHECKED_CAST")
        fun getFactory(context: Context, meta: Meta): DataLoader<Any> {
            return if (meta.hasValue("dataLoaderClass")) {
                try {
                    Class.forName(meta.getString("dataLoaderClass")).getConstructor().newInstance() as DataLoader<Any>
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }

            } else {
                meta.optString(FACTORY_TYPE_KEY).flatMap { loader ->
                    context.serviceStream(DataLoader::class.java)
                            .filter { it -> it.name == loader }
                            .findFirst() ?: error("DataLoader with type $loader not found")
                }.orElse(DummyDataFactory(Any::class.java)) as DataLoader<Any>
            }
        }
    }
}
