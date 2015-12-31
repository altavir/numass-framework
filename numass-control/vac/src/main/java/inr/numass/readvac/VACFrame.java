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
package inr.numass.readvac;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.encoder.Encoder;
import hep.dataforge.context.GlobalContext;
import hep.dataforge.data.DataPoint;
import hep.dataforge.io.BasicIOManager;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.data.DynamicPlottable;
import hep.dataforge.plots.data.DynamicPlottableSet;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueType;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.AbstractButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.XYPlot;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Darksnake
 */
public class VACFrame extends javax.swing.JFrame {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static VACFrame display(VACManager daemon) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VACFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        VACFrame frame = new VACFrame();
        frame.daemon = daemon;
        boolean p1Available = daemon.p1Available();
        frame.p1Power.setEnabled(p1Available);
        if (p1Available) {
            try {
                frame.updateP1PowerState(daemon.getP1PowerState());
            } catch (P1ControlException ex) {
                LoggerFactory.getLogger("COM-P1").error(ex.getMessage());
            }
        } else {
            frame.p1Power.setText("P1 недоступен");
        }

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent winEvt) {
                try {
                    daemon.close();
                } catch (Exception ex) {
                    LoggerFactory.getLogger(VACFrame.class).error(null, ex);
                }
                System.exit(0);
            }
        });

//        /* Create and displayPoint the form */
//        SwingUtilities.invokeLater(() -> {
//            frame.setVisible(true);
////            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        });
        frame.setVisible(true);
        return frame;
    }

    private VACManager daemon;

    private JFreeChartFrame plotFrame;
    private DynamicPlottableSet plottables;
//    private int savedSplitHeight = 600;

    /**
     * Creates new form VACFrame
     */
    private VACFrame() {
        setTitle("Numass vacuum measurement view");
        initComponents();
        split.getRightComponent().setMinimumSize(new Dimension());
        split.setDividerLocation(1.0);
        GlobalContext.instance().attachIoManager(new BasicIOManager(new TextAreaOutputStream(consoleBox, "CONSOLE")));

        JTextAreaAppender app = new JTextAreaAppender(consoleBox);
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        app.setContext(lc);
        app.start();
        lc.getLogger("ROOT").addAppender(app);

        plottables = setupPlot();
        displayChart();
    }

    private DynamicPlottableSet setupPlot() {
        DynamicPlottable p1 = DynamicPlottable.build("P1", "timestamp", "pressure", "RED", 2.5);
        DynamicPlottable p2 = DynamicPlottable.build("P2", "timestamp", "pressure", "BLUE", 2.5);
        DynamicPlottable p3 = DynamicPlottable.build("P3", "timestamp", "pressure", "GREEN", 2.5);
        DynamicPlottable px = DynamicPlottable.build("Px", "timestamp", "pressure", "MAGENTA", 2.5);
        return new DynamicPlottableSet(p1, p2, p3, px);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        valuesPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        p1Label = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        p2Label = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        p3Label = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        pxLabel = new javax.swing.JLabel();
        timeLabel = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        optionsPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        delayBox = new javax.swing.JComboBox();
        rangeBox = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        p1Power = new javax.swing.JToggleButton();
        split = new javax.swing.JSplitPane();
        chartPannel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        consoleBox = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(800, 180));
        setName("mainFrame"); // NOI18N
        setPreferredSize(new java.awt.Dimension(1052, 600));
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        valuesPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        valuesPanel.setMinimumSize(new java.awt.Dimension(0, 90));

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("P1");

        p1Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        p1Label.setForeground(java.awt.Color.red);
        p1Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        p1Label.setText("EMPTY");

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("P2");

        p2Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        p2Label.setForeground(java.awt.Color.blue);
        p2Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        p2Label.setText("EMPTY");

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("P3");

        p3Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        p3Label.setForeground(java.awt.Color.green);
        p3Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        p3Label.setText("EMPTY");

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Px");

        pxLabel.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        pxLabel.setForeground(java.awt.Color.magenta);
        pxLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pxLabel.setText("EMPTY");

        timeLabel.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        timeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        timeLabel.setText("EMPTY");

        jLabel5.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("time");

        javax.swing.GroupLayout valuesPanelLayout = new javax.swing.GroupLayout(valuesPanel);
        valuesPanel.setLayout(valuesPanelLayout);
        valuesPanelLayout.setHorizontalGroup(
            valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(valuesPanelLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(timeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 122, Short.MAX_VALUE)
                .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(p1Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
                .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(p2Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
                .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(p3Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 106, Short.MAX_VALUE)
                .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pxLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(53, Short.MAX_VALUE))
        );
        valuesPanelLayout.setVerticalGroup(
            valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(valuesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(valuesPanelLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pxLabel))
                    .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(valuesPanelLayout.createSequentialGroup()
                            .addComponent(jLabel3)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(p3Label))
                        .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(valuesPanelLayout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(p2Label))
                            .addGroup(valuesPanelLayout.createSequentialGroup()
                                .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(p1Label)
                                    .addComponent(timeLabel))))))
                .addContainerGap())
        );

        getContentPane().add(valuesPanel);

        optionsPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        optionsPanel.setMaximumSize(new java.awt.Dimension(32767, 50));
        optionsPanel.setMinimumSize(new java.awt.Dimension(0, 50));
        optionsPanel.setPreferredSize(new java.awt.Dimension(1052, 50));

        jLabel6.setText("Частота обновления (с):");

        delayBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "5", "10", "30", "60" }));
        delayBox.setSelectedIndex(1);
        delayBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                delayBoxItemStateChanged(evt);
            }
        });

        rangeBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "10", "30", "60", "180", "300" }));
        rangeBox.setSelectedIndex(2);
        rangeBox.setToolTipText("");
        rangeBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                rangeBoxItemStateChanged(evt);
            }
        });
        rangeBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rangeBoxActionPerformed(evt);
            }
        });

        jLabel7.setText("Максимальный диапазон (мин):");

        p1Power.setText("Холодный катод на P1");
        p1Power.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                p1PowerActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout optionsPanelLayout = new javax.swing.GroupLayout(optionsPanel);
        optionsPanel.setLayout(optionsPanelLayout);
        optionsPanelLayout.setHorizontalGroup(
            optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionsPanelLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(delayBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rangeBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 427, Short.MAX_VALUE)
                .addComponent(p1Power)
                .addGap(60, 60, 60))
        );
        optionsPanelLayout.setVerticalGroup(
            optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(rangeBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(delayBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(p1Power))
                .addGap(40, 40, 40))
        );

        getContentPane().add(optionsPanel);

        split.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(1.0);
        split.setAlignmentX(0.5F);
        split.setAlignmentY(0.5F);
        split.setOneTouchExpandable(true);

        chartPannel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        chartPannel.setAutoscrolls(true);
        chartPannel.setLayout(new java.awt.BorderLayout());
        split.setTopComponent(chartPannel);

        consoleBox.setEditable(false);
        consoleBox.setColumns(20);
        consoleBox.setLineWrap(true);
        consoleBox.setRows(5);
        scroll.setViewportView(consoleBox);
        consoleBox.getAccessibleContext().setAccessibleParent(split);

        split.setBottomComponent(scroll);

        getContentPane().add(split);

        getAccessibleContext().setAccessibleParent(this);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void delayBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_delayBoxItemStateChanged
        setTimerInterval(Integer.parseInt((String) evt.getItem()));
    }//GEN-LAST:event_delayBoxItemStateChanged

    private void rangeBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_rangeBoxItemStateChanged
        setAutoRange(Integer.parseInt((String) evt.getItem()) * 60);
    }//GEN-LAST:event_rangeBoxItemStateChanged

    private void p1PowerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_p1PowerActionPerformed
        AbstractButton abstractButton = (AbstractButton) evt.getSource();
        daemon.stop();
        boolean selected = abstractButton.getModel().isSelected();
        try {
            updateP1PowerState(daemon.setP1PowerStateOn(selected));
        } catch (P1ControlException ex) {
            LoggerFactory.getLogger(getClass()).error(ex.getMessage());
            try {
                updateP1PowerState(daemon.getP1PowerState());
            } catch (P1ControlException ex1) {
                LoggerFactory.getLogger(getClass()).error(ex1.getMessage());
            }
        }
        daemon.start();
    }//GEN-LAST:event_p1PowerActionPerformed

    private void rangeBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rangeBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rangeBoxActionPerformed

    private void updateP1PowerState(boolean state) {
        p1Power.setSelected(state);
        if (state) {
            p1Power.setText("P1 включен");
        } else {
            p1Power.setText("P1 выключен");
        }
    }

    public void setTimerInterval(int seconds) {
        daemon.setTimerInterval(seconds * 1000);
    }

    public void setAutoRange(int seconds) {
        plottables.setMaxAge(seconds * 1000);
    }

    private void displayChart() {
        SwingUtilities.invokeLater(() -> {
            Meta plotConfig = new MetaBuilder("plotFrame")
                    .setNode(new MetaBuilder("yAxis")
                            .setValue("logAxis", true)
                            .setValue("axisTitle", "pressure")
                            .setValue("axisUnits", "mbar")
                    )
                    .setValue("xAxis.timeAxis", true);
            this.plotFrame = new JFreeChartFrame("pressures", plotConfig).display(chartPannel);
            XYPlot xyPlot = plotFrame.getChart().getXYPlot();

            LogarithmicAxis logAxis = new LogarithmicAxis("Pressure (mbar)");
//        logAxis.setTickUnit(new NumberTickUnit(2));
            logAxis.setMinorTickCount(10);
            logAxis.setExpTickLabelsFlag(true);
            logAxis.setMinorTickMarksVisible(true);
            xyPlot.setRangeAxis(logAxis);
//            xyPlot.getRenderer().setBaseStroke(new BasicStroke(3));
//            xyPlot.setBackgroundPaint(Color.WHITE);
//            xyPlot.setRangeGridlinesVisible(true);
//            xyPlot.setRangeGridlinePaint(Color.BLACK);
//
//            xyPlot.setRangeMinorGridlinesVisible(true);
//            xyPlot.setRangeMinorGridlinePaint(Color.BLACK);

            //adding data to the frame
            plotFrame.addAll(plottables);
        });
        validate();
//        pack();
    }

    private void setMaxAge(int seconds) {
        plottables.setMaxAge(seconds * 1000);
    }

    public void displayPoint(DataPoint point) {
        SwingUtilities.invokeLater(() -> {
            plottables.put(point);
            timeLabel.setText(formatter
                    .format(LocalDateTime
                            .ofInstant(point.getValue("timestamp").timeValue(), ZoneId.of("UTC"))));
            p1Label.setText(normalize(point.getValue("P1")));
            p2Label.setText(normalize(point.getValue("P2")));
            p3Label.setText(normalize(point.getValue("P3")));
            pxLabel.setText(normalize(point.getValue("Px")));
        });
    }

    private String normalize(Value val) {
        if (val.valueType() == ValueType.NUMBER) {
            return String.format("%.2e", val.doubleValue());
        } else {
            return String.format("%s", val.stringValue());
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel chartPannel;
    private javax.swing.JTextArea consoleBox;
    private javax.swing.JComboBox delayBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel optionsPanel;
    private javax.swing.JLabel p1Label;
    private javax.swing.JToggleButton p1Power;
    private javax.swing.JLabel p2Label;
    private javax.swing.JLabel p3Label;
    private javax.swing.JLabel pxLabel;
    private javax.swing.JComboBox rangeBox;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JSplitPane split;
    private javax.swing.JLabel timeLabel;
    private javax.swing.JPanel valuesPanel;
    // End of variables declaration//GEN-END:variables

    private class JTextAreaAppender extends AppenderBase<ILoggingEvent> {

        private Encoder<ILoggingEvent> encoder = new EchoEncoder<ILoggingEvent>();
        private ByteArrayOutputStream out = new ByteArrayOutputStream();
        private JTextArea textArea;

        public JTextAreaAppender(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void start() {
            try {
                encoder.init(out);
            } catch (IOException e) {
            }

            super.start();
        }

        @Override
        public void append(ILoggingEvent event) {
            try {
                encoder.doEncode(event);
                out.flush();
                String line = out.toString();
                textArea.append(line);
                out.reset();
            } catch (IOException e) {
            }
        }

    }
}
