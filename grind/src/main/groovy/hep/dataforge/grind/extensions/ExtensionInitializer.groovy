package hep.dataforge.grind.extensions

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MutableMetaNode
import hep.dataforge.tables.Table
import hep.dataforge.workspace.Workspace

/**
 * A set of dynamic initializers for groovy features. Must be called explicitly at the start of the program.
 */
class ExtensionInitializer {

    /**
     * Add property access to meta nodes
     * @return
     */
    static def initMeta(){
        Meta.metaClass.getProperty = {String name ->
            delegate.getMetaOrEmpty(name)
        }

        MutableMetaNode.metaClass.setProperty = { String name, Object value ->
            if (value instanceof Meta) {
                delegate.setNode(name, (Meta) value)
            } else if (value instanceof Collection) {
                delegate.setNode(name, (Collection<? extends Meta>) value)
            } else if (value.getClass().isArray()) {
                delegate.setNode(name, (Meta[]) value)
            } else {
                throw new RuntimeException("Can't convert ${value.getClass()} to Meta")
            }
        }
    }

    /**
     * Add property access to column tables
     * @return
     */
    static def initTable(){
        Table.metaClass.getProperty = { String propName ->
            def meta = Table.metaClass.getMetaProperty(propName)
            if (meta) {
                meta.getProperty(delegate)
            } else {
                return (delegate as Table).getColumn(propName)
            }
        }
    }

    static def initWorkspace(){
        Workspace.metaClass.methodMissing = {String name, Object args ->
            String str = args.getClass().isArray() ? ((Object[]) args).join(" ") : args.toString()
            return (delegate as Workspace).runTask(name, str)
        }
    }

    static def initAll(){
        initMeta()
        initTable()
        initWorkspace()
    }
}
