/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.server;

import freemarker.template.Template;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.storage.api.ObjectLoader;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.servlet.StorageRatpackHandler;
import hep.dataforge.storage.servlet.Utils;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;

/**
 *
 * @author Alexander Nozik
 */
public class NumassStorageHandler extends StorageRatpackHandler {

    private static DateTimeFormatter formatter
            = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault());

    public NumassStorageHandler(Storage root) {
        super(root);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void renderObjects(Context ctx, ObjectLoader loader) {
        if (NumassRun.RUN_NOTES.equals(loader.getName())) {
            try {
                ctx.getResponse().contentType("text/html");
                Template template = Utils.freemarkerConfig().getTemplate("NoteLoader.ftl");

                List<String> notes = getNotes(loader).limit(100).map(note -> render(note)).collect(Collectors.toList());

                Map data = new HashMap(2);
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
    protected String pointLoaderPlotOptions(PointLoader loader) {
        if (loader.getName().contains("msp") || loader.getName().contains("vac")) {
            return "                    legend: { \n"
                    + "                        position: 'bottom' \n"
                    + "                    },\n"
                    + "                    title: '" + loader.getName() + "',\n"
                    + "                    vAxis:{\n"
                    + "                        logScale: true\n"
                    + "                    }";
        } else {
            return super.pointLoaderPlotOptions(loader);
        }
    }

    private String render(NumassNote note) {
        return String.format("<strong id=\"%s\">%s</strong> %s", note.ref(), formatter.format(note.time()), note.content());
    }

    /**
     * Stream of notes in the last to first order
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    private Stream<NumassNote> getNotes(ObjectLoader noteLoader) {
        return noteLoader.fragmentNames().stream().<NumassNote>map(new Function<String, NumassNote>() {
            @Override
            public NumassNote apply(String name) {
                try {
                    return (NumassNote) noteLoader.pull(name);
                } catch (StorageException ex) {
                    return (NumassNote) null;
                }
            }
        }).sorted(new Comparator<NumassNote>() {
            @Override
            public int compare(NumassNote o1, NumassNote o2) {
                return -o1.time().compareTo(o2.time());
            }
        });
    }

}