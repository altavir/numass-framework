/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.fx.FXUtils;
import hep.dataforge.meta.Meta;
import hep.dataforge.names.Named;
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
        tabs.values().stream().map((tab) -> {
            tab.close();
            return tab;
        }).forEach((tab) -> {
            Platform.runLater(() -> getTabs().remove(tab));
        });
    }

    public synchronized void closeTab(String name) {
        FXUtils.runNow(() -> {
            tabs.get(name).close();
            getTabs().remove(tabs.get(name));
            tabs.remove(name);
        });
    }

    public synchronized TextOutputTab buildTextOutput(String name) {
        TextOutputTab out = new TextOutputTab(name);
        FXUtils.runNow(() -> {
            if (tabs.containsKey(name)) {
                tabs.get(name).close();
                getTabs().remove(tabs.get(name));
                tabs.replace(name, out);
            }
            getTabs().add(out);
        });
        return out;
    }

    public synchronized PlotFrame buildPlotOutput(String name, Meta meta) {
        PlotOutputTab out = new PlotOutputTab("plot::" + name, meta);
        FXUtils.runNow(() -> {
            if (tabs.containsKey(name)) {
                tabs.get(name).close();
                getTabs().remove(tabs.get(name));
                tabs.replace(name, out);
            }
            getTabs().add(out);
        });
        return out.getFrame();
    }

}
