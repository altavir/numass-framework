/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.server;

import hep.dataforge.exceptions.StorageException;
import hep.dataforge.storage.api.Loader;
import hep.dataforge.storage.api.StateLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.JSONMetaWriter;
import static inr.numass.server.HandlerUtils.*;
import inr.numass.storage.NumassStorage;
import ratpack.handling.Context;
import ratpack.handling.Handler;

/**
 *
 * @author Alexander Nozik
 */
public class NumassRootHandler implements Handler {

    private final JSONMetaWriter writer = new JSONMetaWriter();

    NumassServer server;

    public NumassRootHandler(NumassServer server) {
        this.server = server;
    }

    @Override
    public void handle(Context c) throws Exception {
        c.getResponse().contentType("text/html");
        StringBuilder b = new StringBuilder();
        renderHTMLHeader(b);
        b.append("<h1> Server configuration </h1>\n");
        if (!server.meta().isEmpty()) {
            b.append("<h3> Server metadata: </h3>\n");
            b.append(writer.writeString(server.meta()));
            b.append("\n");
        }
        if (server.getRootState() != null) {
            b.append("<h3> Current root state: </h3>\n");
            renderStates(b, server.getRootState());
        }
        if (server.getRun() != null) {
            b.append("<h1> Current run configuration </h1>\n");
            if (!server.getRun().meta().isEmpty()) {
                b.append("<h3> Run metadata: </h3>\n");
                b.append(writer.writeString(server.getRun().meta()));
                b.append("\n");
            }
            StateLoader runStates = server.getRun().getStates();
            if (!runStates.isEmpty()) {
                b.append("<h3> Current run state: </h3>\n");
                renderStates(b, runStates);
            }

            b.append("<h2> Current run storage content: </h2>\n");
            NumassStorage storage = server.getRun().getStorage();
            try {
                renderStorage(c, b, storage);
            } catch (StorageException ex) {
                b.append("\n<strong>Error reading sotrage structure!!!</strong>\n");
            }
        }
        renderHTMLFooter(b);
        c.render(b);
    }

    private void renderStorage(Context ctx, StringBuilder b, Storage storage) throws StorageException {
        b.append("<div class=\"shifted\">\n");
        for (Storage shelf : storage.shelves().values()) {
            b.append(String.format("<p><strong>+ %s</strong></p>%n", shelf.getName()));
            renderStorage(ctx, b, shelf);
        }
        b.append("<div class=\"shifted\">\n");
        for (Loader loader : storage.loaders().values()) {
            renderLoader(ctx, b, loader);
        }
        b.append("</div>\n");
        b.append("</div>\n");
    }

    private void renderLoader(Context ctx, StringBuilder b, Loader loader) {
        String href = "/storage?path=" + loader.getFullPath();
        b.append(String.format("<p><a href=\"%s\">%s</a> (%s)</p>", href, loader.getName(), loader.getType()));
    }
}
