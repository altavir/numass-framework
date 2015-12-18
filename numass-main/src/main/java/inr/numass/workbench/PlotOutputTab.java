/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.meta.Meta;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import javafx.scene.layout.AnchorPane;

public class PlotOutputTab extends OutputTab {

    private JFreeChartFrame frame;

    public PlotOutputTab(String name, Meta meta) {
        super(name);
        AnchorPane pane = new AnchorPane();
        frame = new JFreeChartFrame(name, meta, pane);
        setContent(pane);
    }

    public PlotOutputTab(String name, String title, Meta meta) {
        super(name, title);
        AnchorPane pane = new AnchorPane();
        frame = new JFreeChartFrame(name, meta, pane);
        setContent(pane);
    }

    @Override
    public void close() {

    }

    @Override
    public void clear() {

    }

    public JFreeChartFrame getFrame() {
        return frame;
    }
    
    

}
