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
package inr.numass;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import hep.dataforge.context.Context;
import hep.dataforge.io.BasicIOManager;
import hep.dataforge.names.Name;
import hep.dataforge.utils.ReferenceRegistry;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Darksnake
 */
public class NumassIO extends BasicIOManager {

    public static final String NUMASS_OUTPUT_CONTEXT_KEY = "numass.outputDir";

    ReferenceRegistry<OutputStream> registry = new ReferenceRegistry<>();
//    FileAppender<ILoggingEvent> appender;


    @Override
    public void attach(Context context) {
        super.attach(context);
    }

    @Override
    public Appender<ILoggingEvent> createLoggerAppender() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setFile(new File(getWorkDirectory().toFile(), meta().getString("logFileName", "numass.log")).toString());
        appender.setEncoder(ple);
        return appender;
    }

    @Override
    public void detach() {
        super.detach();
        registry.forEach(it -> {
            try {
                it.close();
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("Failed to close output", e);
            }
        });
    }

    @Override
    public OutputStream out(Name stage, Name name) {
        List<String> tokens = new ArrayList<>();
        if (getContext().hasValue("numass.path")) {
            String path = getContext().getString("numass.path");
            if (path.contains(".")) {
                tokens.addAll(Arrays.asList(path.split(".")));
            } else {
                tokens.add(path);
            }
        }

        if (stage != null && stage.length() != 0) {
            tokens.addAll(Arrays.asList(stage.asArray()));
        }

        String dirName = String.join(File.separator, tokens);
        String fileName = name.removeNameSpace().toString();
        OutputStream out = buildOut(getWorkDirectory(), dirName, fileName);
        registry.add(out);
        return out;
    }

    private OutputStream buildOut(Path parentDir, String dirName, String fileName) {
        Path outputFile;

        if (!Files.exists(parentDir)) {
            throw new RuntimeException("Working directory does not exist");
        }
        try {
            if (dirName != null && !dirName.isEmpty()) {
                parentDir = parentDir.resolve(dirName);
                Files.createDirectories(parentDir);
            }

//        String output = source.meta().getString("output", this.meta().getString("output", fileName + ".onComplete"));
            outputFile = parentDir.resolve(fileName);

            if (getContext().getBoolean("numass.consoleOutput", false)) {
                return new TeeOutputStream(Files.newOutputStream(outputFile), System.out);
            } else {
                return Files.newOutputStream(outputFile);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
