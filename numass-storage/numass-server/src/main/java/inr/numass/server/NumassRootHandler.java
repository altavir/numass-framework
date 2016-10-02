/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.server;

import freemarker.template.Template;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.storage.api.Loader;
import hep.dataforge.storage.api.StateLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.JSONMetaWriter;
import hep.dataforge.storage.servlet.Utils;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static inr.numass.server.HandlerUtils.renderStates;

/**
 *
 * @author Alexander Nozik
 */
@SuppressWarnings("unchecked")
public class NumassRootHandler implements Handler {

    private final JSONMetaWriter writer = new JSONMetaWriter();

    NumassServer server;

    public NumassRootHandler(NumassServer server) {
        this.server = server;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            ctx.getResponse().contentType("text/html");
            Template template = Utils.freemarkerConfig().getTemplate("NumassRoot.ftl");

            Map data = new HashMap(6);
            if (!server.meta().isEmpty()) {
                data.put("serverMeta", writer.writeString(server.meta()));
            }

            if (server.getRootState() != null) {
                data.put("serverRootState", renderStates(server.getRootState()));
            }

            if (server.getRun() != null) {
                data.put("runPresent", true);
                if (!server.getRun().meta().isEmpty()) {
                    data.put("runMeta", writer.writeString(server.getRun().meta()));
                }

                StateLoader runState = server.getRun().getStates();
                if (!runState.isEmpty()) {
                    data.put("runState", renderStates(runState));
                }

                try {
                    StringBuilder b = new StringBuilder();
                    renderStorage(ctx, b, server.getRun().getStorage());
                    data.put("storageContent", b.toString());
                } catch (Exception ex) {
                    data.put("storageContent", ex.toString());
                }
            } else {
                data.put("runPresent", false);
            }

            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);

            ctx.render(stringWriter.toString());

        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass()).error("Error rendering storage tree");
            ctx.render(ex.toString());
        }
    }

    private void renderStorage(Context ctx, StringBuilder b, Storage storage){
        try {
            b.append("<div class=\"shifted\">\n");
            storage.shelves().values().stream().sorted(Comparator.comparing(it -> it.getName())).forEach(shelf -> {
                b.append(String.format("<p><strong>+ %s</strong></p>%n", shelf.getName()));
                renderStorage(ctx, b, shelf);
            });

            b.append("<div class=\"shifted\">\n");

            storage.loaders().values().stream().sorted(Comparator.comparing(it->it.getName())).forEach(loader -> renderLoader(ctx, b, loader));

            b.append("</div>\n");
            b.append("</div>\n");
        }catch (StorageException ex){
            throw new RuntimeException(ex);
        }
    }

    private void renderLoader(Context ctx, StringBuilder b, Loader loader) {
        String href = "/storage?path=" + loader.getPath();
        b.append(String.format("<p><a href=\"%s\">%s</a> (%s)</p>", href, loader.getName(), loader.getType()));
    }
}
