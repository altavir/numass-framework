package hep.dataforge.grind.workspace

import groovy.transform.CompileStatic
import hep.dataforge.Named
import hep.dataforge.context.Context
import hep.dataforge.data.*
import hep.dataforge.goals.GeneratorGoal
import hep.dataforge.goals.Goal
import hep.dataforge.goals.StaticGoal
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindMetaBuilder
import hep.dataforge.meta.Meta

/**
 * A specification to build data node. Not thread safe
 */
@CompileStatic
class DataNodeSpec {
//
//    /**
//     * Put a static resource as data
//     * @param place
//     * @param path
//     * @return
//     */
//    def resource(String place, String path) {
//        URI uri = URI.create(path)
//        builder.data(place, Data.buildStatic(uri))
//    }
//

    static DataNode buildNode(Context context,
                              @DelegatesTo(value = DataNodeSpec, strategy = Closure.DELEGATE_ONLY) Closure cl) {
        def spec = new DataNodeSpec(context, "data", Object.class)
        def code = cl.rehydrate(spec, null, null)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        return spec.build()
    }

    private final Context context;
    private final String name;
    private final Class type;
    private Meta meta = Meta.empty()
    private DataNodeBuilder tree;

    DataNodeSpec(Context context, String name, Class type = Object.class) {
        this.context = context
        this.name = name
        this.type = type
        tree = DataTree.edit(Object)
        tree.setName(name)
    }

    void meta(Map values = [:], @DelegatesTo(GrindMetaBuilder) Closure cl = null) {
        this.meta = Grind.buildMeta("meta", values, cl);
    }

    void load(Meta meta) {
        if (!tree.isEmpty()) {
            throw new RuntimeException("Trying to load data into non-empty tree. Load should be called first.")
        }
        //def newRoot = node("", DataLoader.SMART.build(context, meta))
        DataNode<?> node = new SmartDataLoader().build(context, meta)
        tree = node.edit()
    }

    void load(Map values = [:], String nodeName = "", @DelegatesTo(GrindMetaBuilder) Closure cl = null) {
        load(Grind.buildMeta(nodeName, values, cl))
    }


    void file(String place, String path, @DelegatesTo(GrindMetaBuilder) Closure fileMeta = null) {
        item(place, DataUtils.INSTANCE.readFile(context.getFile(path), Grind.buildMeta(fileMeta)))
    }

    void item(NamedData data) {
        tree.add(data)
    }

    void item(String name, Data data) {
        tree.putData(name, data, false)
    }

    void item(String name, @DelegatesTo(ItemSpec) Closure cl) {
        ItemSpec spec = new ItemSpec(name)
        def code = cl.rehydrate(spec, this, this)
        code.resolveStrategy = Closure.DELEGATE_FIRST
        def res = code.call()
        if (res) {
            spec.setValue(res)
        }
        item(spec.build())
    }

    void item(String name, Object obj) {
        item(name, Data.buildStatic(obj))
    }

    void node(DataNode node) {
        tree.add(node)
    }

    void node(String name, DataNode node) {
        tree.putNode(name, node)
    }

    void node(String name, Class type = Object.class, @DelegatesTo(DataNodeSpec) Closure cl) {
        DataNodeSpec spec = new DataNodeSpec(context, name, type)
        def code = cl.rehydrate(spec, this, this)
        code.resolveStrategy = Closure.DELEGATE_FIRST
        code.call()
        tree.add(spec.build());
    }

    /**
     * Load static data from map
     * @param items
     * @return
     */
    void items(Map<String, ?> items) {
        items.each { key, value ->
            item(key, Data.buildStatic(value))
        }
    }

    /**
     *        Load static data from collection of Named objects
     */
    void items(Collection<? extends Named> something) {
        something.each {
            item(it.name, Data.buildStatic(it))
        }
    }

    private DataNode build() {
        tree.setMeta(meta)
        return tree.build();
    }


    static class ItemSpec {
        private final String name;
        Class type = Object;
        private Meta meta = Meta.empty()
        private Goal goal = null;

        ItemSpec(String name) {
            this.name = name
        }

        void meta(Map values = [:], @DelegatesTo(GrindMetaBuilder) Closure cl = null) {
            this.meta = Grind.buildMeta("meta", values, cl);
        }

        void setValue(Object obj) {
            this.goal = new StaticGoal(obj);
            this.type = obj.class;
        }

        void setValue(Class type, Closure cl) {
            this.type = type
            this.goal = new GeneratorGoal({ cl.call() })
        }

        void setValue(Closure cl) {
            setValue(Object.class, cl)
        }

        private NamedData build() {
            return new NamedData(name, type, goal, meta);
        }
    }
}
