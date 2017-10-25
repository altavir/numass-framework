package inr.numass.models

import hep.dataforge.stat.models.Model
import hep.dataforge.stat.models.ModelDescriptor
import hep.dataforge.stat.models.ModelFactory
import hep.dataforge.utils.ContextMetaFactory


fun model(name: String, descriptor: ModelDescriptor? = null, factory: ContextMetaFactory<Model>): ModelFactory {
    return ModelFactory.build(name, descriptor, factory);
}