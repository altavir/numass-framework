/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.meta.Meta;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;

public class PlotOutputTab extends OutputTab {

    private final JFreeChartFrame frame;

    public PlotOutputTab(String name, Meta meta) {
        super(name);
        PlotContainer container = new PlotContainer();
        frame = new JFreeChartFrame(meta);
        container.setPlot(frame);
//        AnchorPane pane = new AnchorPane();
//        frame = new JFreeChartFrame(name, meta).display(pane);
        setContent(container);
    }

    public PlotOutputTab(String name, String title, Meta meta) {
        super(name, title);
        PlotContainer container = new PlotContainer();
        frame = new JFreeChartFrame(meta);
        container.setPlot(frame);        
        setContent(container);
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
