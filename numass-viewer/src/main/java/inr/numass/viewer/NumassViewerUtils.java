/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.viewer;

import java.awt.BorderLayout;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.AnchorPane;
import javax.swing.JPanel;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

/**
 *
 * @author darksnake
 */
public class NumassViewerUtils {

    /**
     * Display given JFreeChart in the FX container
     *
     * @param container
     * @param chart
     */
    public static void displayPlot(AnchorPane container, JFreeChart chart) {
        SwingNode viewer = new SwingNode();
        JPanel panel = new JPanel(new BorderLayout(), true);
        panel.removeAll();
        panel.add(new ChartPanel(chart));
        panel.revalidate();
        panel.repaint();
        viewer.setContent(panel);

        AnchorPane.setBottomAnchor(viewer, 0d);
        AnchorPane.setTopAnchor(viewer, 0d);
        AnchorPane.setLeftAnchor(viewer, 0d);
        AnchorPane.setRightAnchor(viewer, 0d);
        container.getChildren().add(viewer);
    }

}
