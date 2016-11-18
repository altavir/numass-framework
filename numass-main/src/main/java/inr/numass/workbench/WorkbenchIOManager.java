/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.io.BasicIOManager;
import hep.dataforge.io.IOManager;
import hep.dataforge.names.Name;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An IOManager wrapper that redirects output to appropriate FX components
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class WorkbenchIOManager extends BasicIOManager {
    
    private final IOManager manager;
    private final StagePaneHolder holder;

    public WorkbenchIOManager(IOManager manager, StagePaneHolder holder) {
        this.manager = manager;
        this.holder = holder;
    }

    @Override
    public File getFile(String path) {
        return manager.getFile(path);
    }

    @Override
    public File getRootDirectory() {
        return manager.getRootDirectory();
    }

    @Override
    public InputStream in() {
        return manager.in();
    }

    @Override
    public InputStream in(String path) {
        return manager.in(path);
    }

    @Override
    public OutputStream out(Name stage, Name name) {
        //split output between parent output and holder output
        OutputStream primary = holder.getStagePane(stage.toString()).buildTextOutput(name.toString()).getStream();
        OutputStream secondary = manager.out(stage, name);
        return new TeeOutputStream(primary, secondary);
    }

    @Override
    public OutputStream out() {
        return manager.out();
//        return new ConsoleStream(holder.getLogArea(), new PrintStream(manager.onComplete()));
    }
    
}
