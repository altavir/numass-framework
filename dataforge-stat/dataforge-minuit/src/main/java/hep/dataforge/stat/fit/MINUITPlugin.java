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
package hep.dataforge.stat.fit;

import hep.dataforge.context.*;
import hep.dataforge.io.history.Chronicle;
import hep.dataforge.meta.Meta;
import hep.dataforge.providers.Provides;
import hep.dataforge.providers.ProvidesNames;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Мэнеджер для MINUITа. Пока не играет никакой активной роли кроме ведения
 * внутреннего лога.
 *
 * @author Darksnake
 * @version $Id: $Id
 */
@PluginDef(group = "hep.dataforge", name = "MINUIT",
        dependsOn = {"hep.dataforge:fitting"},
        info = "The MINUIT fitter engine for DataForge fitting")
public class MINUITPlugin extends BasicPlugin {

    /**
     * Constant <code>staticLog</code>
     */
    private static final Chronicle staticLog = new Chronicle("MINUIT-STATIC", Global.INSTANCE.getHistory());

    /**
     * <p>
     * clearStaticLog.</p>
     */
    public static void clearStaticLog() {
        staticLog.clear();
    }

    /**
     * <p>
     * logStatic.</p>
     *
     * @param str  a {@link java.lang.String} object.
     * @param pars a {@link java.lang.Object} object.
     */
    public static void logStatic(String str, Object... pars) {
        if (staticLog == null) {
            throw new IllegalStateException("MINUIT log is not initialized.");
        }
        staticLog.report(str, pars);
        LoggerFactory.getLogger("MINUIT").info(String.format(str, pars));
//        Out.out.printf(str,pars);
//        Out.out.println();
    }

    @Override
    public void attach(@NotNull Context context) {
        super.attach(context);
        clearStaticLog();
    }


    @Provides(Fitter.FITTER_TARGET)
    public Fitter getFitter(String fitterName) {
        if (fitterName.equals("MINUIT")) {
            return new MINUITFitter();
        } else {
            return null;
        }
    }

    @ProvidesNames(Fitter.FITTER_TARGET)
    public List<String> listFitters() {
        return Collections.singletonList("MINUIT");
    }

    @Override
    public void detach() {
        clearStaticLog();
        super.detach();
    }

    public static class Factory extends PluginFactory {
        @Override
        public Plugin build(Meta meta) {
            return new MINUITPlugin();
        }

        @Override
        public Class<? extends Plugin> getType() {
            return MINUITPlugin.class;
        }
    }

}
