package hep.dataforge.grind.workspace

import groovy.transform.CompileStatic
import hep.dataforge.context.Context
import hep.dataforge.context.ContextBuilder
import hep.dataforge.grind.Grind
import hep.dataforge.meta.Meta

/**
 * A specification to builder context via grind workspace definition
 */
@CompileStatic
class ContextSpec {
    private final Context parent;

    String name = "workspace"
    Map properties = new HashMap()
    Map<String, Meta> pluginMap = new HashMap<>()

    ContextSpec(Context parent) {
        this.parent = parent
    }

    Context build() {
        //using current context as a parent for workspace context
        Context res = new ContextBuilder(name, parent).build()
        properties.each { key, value -> res.setValue(key.toString(), value) }
        pluginMap.forEach { String key, Meta meta ->
            res.getPlugins().load(key, meta)
        }
        return res
    }

    def properties(Closure cl) {
        def spec = [:]//new PropertySetSpec();
        def code = cl.rehydrate(spec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        properties.putAll(spec)
    }

    def plugin(String key) {
        pluginMap.put(key, Meta.empty())
    }

    def plugin(String key, Closure cl) {
        pluginMap.put(key, Grind.buildMeta(cl))
    }

    def rootDir(String path) {
        properties.put("rootDir", path)
    }
}
