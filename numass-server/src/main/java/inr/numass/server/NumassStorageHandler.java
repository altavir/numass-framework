/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.server;

import freemarker.template.Template;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.server.ServerManager;
import hep.dataforge.server.ServletUtils;
import hep.dataforge.server.storage.StorageRatpackHandler;
import hep.dataforge.storage.api.ObjectLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.api.TableLoader;
import inr.numass.data.api.NumassSet;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static inr.numass.server.NumassServerUtils.getNotes;

/**
 * @author Alexander Nozik
 */
public class NumassStorageHandler extends StorageRatpackHandler {

    private static DateTimeFormatter formatter
            = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault());

    public NumassStorageHandler(ServerManager manager, Storage root) {
        super(manager, root);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void renderObjects(Context ctx, ObjectLoader<?> loader) {
        if (loader instanceof NumassSet) {

        } else if (NumassRun.RUN_NOTES.equals(loader.getName())) {
            try {
                ObjectLoader<NumassNote> noteLoader = (ObjectLoader<NumassNote>) loader;
                ctx.getResponse().contentType("text/html");
                Template template = ServletUtils.freemarkerConfig().getTemplate("NoteLoader.ftl");

                List<String> notes = getNotes(noteLoader).limit(100).map(this::render).collect(Collectors.toList());

                Map<String, Object> data = new HashMap<>(2);
                data.put("notes", notes);

                StringWriter writer = new StringWriter();
                template.process(data, writer);

                ctx.render(writer.toString());
            } catch (Exception ex) {
                LoggerFactory.getLogger(getClass()).error("Failed to render template", ex);
                ctx.render(ex.toString());
            }
        } else {
            super.renderObjects(ctx, loader);
        }
    }

    @Override
    protected MetaBuilder pointLoaderPlotOptions(TableLoader loader) {
        MetaBuilder builder = super.pointLoaderPlotOptions(loader);
        if (loader.getName().startsWith("msp")
                || loader.getName().startsWith("vac")
                || loader.getName().startsWith("cryotemp")) {
            builder.putValue("legend.position", "bottom");
            builder.putValue("title", "\"" + loader.getName() + "\"");
            builder.putNode(new MetaBuilder("vAxis")
                    .putValue("logScale", true)
                    .putValue("format", "scientific")
            );
            builder.putNode(new MetaBuilder("hAxis")
                            .putValue("title", "timestamp")
//                    .putValue("gridlines.count", -1)
//                    .putValues("gridlines.units.days.format", "MMM dd")
//                    .putValues("gridlines.units.hours.format", "HH:mm", "ha")
            );
        }
        return builder;
    }


    private String render(NumassNote note) {
        return String.format("<strong id=\"%s\">%s</strong> %s", note.ref(), formatter.format(note.time()), note.content());
    }

}
