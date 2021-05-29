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

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.SimpleMetaMorph;

import java.util.Arrays;


/**
 * The description of fit stage to run
 * @author Alexander Nozik
 */
public class FitStage extends SimpleMetaMorph {

    public static final String STAGE_KEY = "stage";

    public static final String TASK_RUN = "fit";
    public static final String TASK_SINGLE = "single";
    public static final String TASK_COVARIANCE = "covariance";

    public static final String FIT_STAGE_TYPE = "action";
    public static final String FREE_PARAMETERS = "freePars";
    public static final String ENGINE_NAME = "engine";
    public static final String METHOD_NAME = "method";
    public static final String DEFAULT_METHOD_NAME = "default";


    public FitStage(Meta taskAnnotation) {
        super(taskAnnotation);
    }

    public FitStage(String engineName, String taskName, String methodName, String[] freePars) {
        this(new MetaBuilder(STAGE_KEY)
                .putValue(FIT_STAGE_TYPE, taskName)
                .putValue(ENGINE_NAME, engineName)
                .putValue(METHOD_NAME, methodName)
                .putValues(FREE_PARAMETERS, freePars)
                .build()
        );
    }

    public FitStage(String engineName, String taskName, String[] freePars) {
        this(engineName, taskName, DEFAULT_METHOD_NAME, freePars);
    }

    public FitStage(String engineName, String taskName) {
        this(new MetaBuilder(STAGE_KEY)
                .putValue(FIT_STAGE_TYPE, taskName)
                .putValue(ENGINE_NAME, engineName)
                .build()
        );
    }

    public FitStage(String taskName) {
        this(new MetaBuilder(STAGE_KEY).putValue(FIT_STAGE_TYPE, taskName).build());
    }

    public String getEngineName() {
        return getMeta().getString(ENGINE_NAME, QOWFitter.QOW_ENGINE_NAME);
    }

    public String getMethodName() {
        return getMeta().getString(METHOD_NAME, DEFAULT_METHOD_NAME);
    }

    /**
     * Если передается null или пустой массив, то считается что свободны все
     * параметры Данный метод не учитывает априорной информации. Параметр, по
     * которому задана априорная информация считается свободным в смысле фита.
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getFreePars() {
        if (getMeta().hasValue(FREE_PARAMETERS)) {
            return getMeta().getStringArray(FREE_PARAMETERS);
        } else {
            return new String[0];
        }
    }

    public String getType() {
        return getMeta().getString(FIT_STAGE_TYPE, "fit");
    }

    @Override
    public String toString() {
        String parameters;
        String[] freePars = getFreePars();
        
        if(freePars == null || freePars.length == 0){
            parameters = "all parameters";
        } else {
            parameters = Arrays.toString(freePars);
        }
            
        return getType() + "(" + parameters + ")";
    }

}
