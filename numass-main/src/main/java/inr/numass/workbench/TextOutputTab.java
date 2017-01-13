/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.fx.FXDataOutputPane;
import javafx.event.Event;

import java.io.OutputStream;

/**
 * A text output tab. Basically it is attached to IOManager::onComplete
 *
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public class TextOutputTab extends OutputTab {

    private final FXDataOutputPane out;

    /**
     * Create new stream output tab
     *
     * @param name
     * @param stream outputStream to which output should be redirected after
     * displaying in window
     */
    public TextOutputTab(String name) {
        super(name);
//        onComplete = new DataOutputPane();
        out = new FXDataOutputPane();
        setContent(out.getRoot());
        setOnClosed((Event event) -> close());
    }

    @Override
    public void clear() {
        out.clear();
    }

    @Override
    public final void close() {
        clear();
    }

    public OutputStream getStream() {
        return out.getStream();
    }

}
