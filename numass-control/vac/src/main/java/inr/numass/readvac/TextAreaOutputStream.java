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

import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Darksnake
 */
public class TextAreaOutputStream extends OutputStream {

    private static final int MAXLINES = 500;

    private final JTextArea textArea;
    private final StringBuilder sb = new StringBuilder();
    private final String title;

//    public static IOManager getTextAreaIO(Context context, JTextArea area){
//        return new BasicIOManager(context, new TextAreaOutputStream(area, "system"));
//    }

    public TextAreaOutputStream(final JTextArea textArea, String title) {
        this.textArea = textArea;
        this.title = title;
        sb.append(title).append("> ");
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\r') {
            return;
        }
        if (b == '\n') {
            final String text = sb.toString() + "\n";
            SwingUtilities.invokeLater(() -> {
                textArea.append(text);
                while (textArea.getLineCount() >= MAXLINES) {
                    try {
                        textArea.getDocument().remove(0, Utilities.getRowEnd(textArea, 1));
                    } catch (BadLocationException ex) {
                        LoggerFactory.getLogger(getClass()).error(null, ex);
                        break;
                    }
                }
            });
            sb.setLength(0);
            sb.append(title).append("> ");
            return;
        }
        sb.append((char) b);
    }
    
}
