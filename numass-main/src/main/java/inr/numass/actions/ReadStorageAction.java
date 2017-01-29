package inr.numass.actions;

import hep.dataforge.actions.OneToManyAction;
import hep.dataforge.context.Context;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.Loader;
import inr.numass.debunch.DebunchReport;
import inr.numass.debunch.FrameAnalizer;
import inr.numass.storage.*;

import java.util.Map;

/**
 * Created by darksnake on 29-Jan-17.
 */
public class ReadStorageAction extends OneToManyAction<NumassStorage, NumassData> {
    @Override
    protected Map<String, Meta> prepareMeta(Context context, String inputName, Laminate meta) {
        return null;
    }

    @Override
    protected NumassData execute(Context context, String inputName, String outputName, NumassStorage input, Laminate meta) {
        try(Loader loader = input.getLoader(outputName)) {
            if (loader instanceof NumassDataLoader) {
                NumassDataLoader nd = (NumassDataLoader) loader;
                return buildData(context, nd, meta);
            } else {
                throw new RuntimeException("Numass loader expected");
            }
        } catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private NumassData buildData(Context context, NumassDataLoader loader, Meta meta) {
        if (meta.hasNode("debunch")) {
            return loader.applyRawTransformation(rp -> debunch(context, rp, meta.getMeta("debunch")));
        } else {
            return loader;
        }
    }

    private NMPoint debunch(Context context, RawNMPoint point, Meta meta) {
        int upper = meta.getInt("upperchanel", RawNMPoint.MAX_CHANEL);
        int lower = meta.getInt("lowerchanel", 0);
        double rejectionprob = meta.getDouble("rejectprob", 1e-10);
        double framelength = meta.getDouble("framelength", 1);
        double maxCR = meta.getDouble("maxcr", 500d);

        double cr = point.selectChanels(lower, upper).getCR();
        if (cr < maxCR) {
            DebunchReport report = new FrameAnalizer(rejectionprob, framelength, lower, upper).debunchPoint(point);
            return new NMPoint(report.getPoint());
        } else {
            return new NMPoint(point);
        }
    }
}
