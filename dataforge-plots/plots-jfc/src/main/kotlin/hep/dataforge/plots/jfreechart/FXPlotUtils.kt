/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.plots.jfreechart

import hep.dataforge.io.envelopes.DefaultEnvelopeType
import hep.dataforge.io.envelopes.DefaultEnvelopeWriter
import hep.dataforge.io.envelopes.xmlMetaType
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotFrame
import javafx.scene.control.MenuItem
import javafx.stage.FileChooser
import javafx.stage.Window
import java.awt.Color
import java.io.FileOutputStream
import java.io.IOException

object FXPlotUtils {
    fun getAWTColor(meta: Meta, def: Color?): Color? {
        return when {
            meta.hasValue("color") -> {
                val fxColor = javafx.scene.paint.Color.valueOf(meta.getString("color"))
                Color(fxColor.red.toFloat(), fxColor.green.toFloat(), fxColor.blue.toFloat())
            }
            else -> def
        }
    }

    fun awtColorToString(color: Color): String {
        val fxColor = javafx.scene.paint.Color.rgb(
                color.red,
                color.green,
                color.blue,
                color.transparency.toDouble()
        )
        return String.format("#%02X%02X%02X",
                (fxColor.red * 255).toInt(),
                (fxColor.green * 255).toInt(),
                (fxColor.blue * 255).toInt())
    }


    /**
     *
     * @param window
     * @param frame
     * @return
     */
    fun getDFPlotExportMenuItem(window: Window?, frame: PlotFrame): MenuItem {
        val dfpExport = MenuItem("DF...")
        dfpExport.setOnAction { _ ->
            val chooser = FileChooser()
            chooser.extensionFilters.setAll(FileChooser.ExtensionFilter("DataForge envelope", "*.df"))
            chooser.title = "Select file to save plot into"
            val file = chooser.showSaveDialog(window)
            if (file != null) {
                try {
                    DefaultEnvelopeWriter(DefaultEnvelopeType.INSTANCE, xmlMetaType)
                            .write(FileOutputStream(file), PlotFrame.Wrapper().wrap(frame))
                } catch (ex: IOException) {
                    throw RuntimeException("Failed to save plot to file", ex)
                }

            }
        }
        return dfpExport
    }

}