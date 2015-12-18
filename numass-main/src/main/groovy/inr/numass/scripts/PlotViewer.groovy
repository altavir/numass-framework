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
package inr.numass.scripts;

import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.slf4j.LoggerFactory;

/**
 * A basic plot viewer for serialized JFreeChart plots
 *
 * @author Darksnake
 */
public class PlotViewer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            for (String arg : args) {
                File ser = new File(arg);
                try {
                    FileInputStream stream = new FileInputStream(ser);
                    ObjectInputStream ostr = new ObjectInputStream(stream);
                    JFreeChartFrame.deserialize(ostr);
                } catch (IOException ex) {
                    LoggerFactory.getLogger(PlotViewer.class).error("IO error during deserialization", ex);
                } catch (ClassNotFoundException ex) {
                    LoggerFactory.getLogger(PlotViewer.class).error("Wrong serialized content type", ex);
                }
            }
        }
    }

}
