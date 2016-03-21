/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.names.Named;
import hep.dataforge.meta.Meta;
import hep.dataforge.plots.PlotFrame;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.control.TabPane;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class StagePane extends TabPane implements Named {

    private String name;
    private final Map<String, OutputTab> tabs = new HashMap<>();

    @Override
    public String getName() {
        return name;
    }

    public void closeAll() {
        for (OutputTab tab : tabs.values()) {
            tab.close();
            Platform.runLater(() -> getTabs().remove(tab));
        }
    }

    public void closeTab(String name) {
        tabs.get(name).close();
        Platform.runLater(() -> getTabs().remove(tabs.get(name)));
    }

    public TextOutputTab buildTextOutput(String name) {
        if (tabs.containsKey(name)) {
            closeTab(name);
        }
        TextOutputTab out = new TextOutputTab(name);
        tabs.put(name, out);
        Platform.runLater(() -> getTabs().add(out));
        return out;
    }

    public PlotFrame buildPlotOutput(String name, Meta meta) {
        if (tabs.containsKey(name)) {
            closeTab(name);
        }
        PlotOutputTab out = new PlotOutputTab("plot::" + name, meta);
        tabs.put(name, out);
        Platform.runLater(() -> getTabs().add(out));
        return out.getFrame();
    }

}
