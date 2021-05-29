package hep.dataforge.tables;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaHolder;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Simple hash map based adapter
 */
public class BasicAdapter extends MetaHolder implements ValuesAdapter {

    private final Map<String, String> mappings = new HashMap<>(6);

    public BasicAdapter(Meta meta) {
        super(meta);
        updateMapping();
    }

    private void updateMapping() {
        MetaUtils.valueStream(getMeta()).forEach(pair -> {
            mappings.put(pair.getFirst().toString(), pair.getSecond().getString());
//
//            if(pair.getKey().endsWith(".value")){
//                mappings.put(pair.getKey().replace(".value",""),pair.getValue().getString());
//            } else {
//                mappings.put(pair.getKey(), pair.getValue().getString());
//            }
        });
    }

    @Override
    public Optional<Value> optComponent(Values values, String component) {
        return values.optValue(getComponentName(component));
    }

    @Override
    public String getComponentName(String component) {
        return mappings.getOrDefault(component, component);
    }

    @Override
    public Stream<String> listComponents() {
        return mappings.keySet().stream();
    }

    @NotNull
    @Override
    public Meta toMeta() {
        if (getClass() == BasicAdapter.class) {
            return getMeta();
        } else {
            //for custom adapters
            return getMeta().getBuilder().putValue("@class", getClass().getName());
        }
    }

}
