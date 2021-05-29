package hep.dataforge.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unmodifiable meta node
 */
public final class SealedNode extends MetaNode<SealedNode> {

    public SealedNode(Meta meta) {
        super(meta.getName());
        meta.getValueNames(true).forEach((valueName) -> {
            super.values.put(valueName,meta.getValue(valueName));
        });

        meta.getNodeNames(true).forEach((nodeName) -> {
            List<SealedNode> item = meta.getMetaList(nodeName).stream()
                    .map(SealedNode::new)
                    .collect(Collectors.toList());
            super.nodes.put(nodeName, new ArrayList<>(item));
        });
    }

    @Override
    public SealedNode getSealed() {
        return this;
    }

    @Override
    protected SealedNode cloneNode(Meta node) {
        return new SealedNode(node);
    }

    @Override
    public SealedNode self() {
        return this;
    }
}
