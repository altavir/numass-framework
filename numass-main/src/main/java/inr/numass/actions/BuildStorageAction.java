package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.meta.Laminate;
import inr.numass.storage.NumassStorage;

import java.net.URI;

/**
 * TODO generalize and move to dataforge-stoage
 * Create a Storage from given URI object
 * Created by darksnake on 29-Jan-17.
 */
@TypedActionDef(name = "buildStorage", inputType = URI.class, outputType = NumassStorage.class)
public class BuildStorageAction extends OneToOneAction<URI, NumassStorage> {
    @Override
    protected NumassStorage execute(Context context, String name, URI input, Laminate inputMeta) {
        return NumassStorage.buildNumassRoot(input, inputMeta.getBoolean("readOnly", true), inputMeta.getBoolean("monitor", false));
    }
}
