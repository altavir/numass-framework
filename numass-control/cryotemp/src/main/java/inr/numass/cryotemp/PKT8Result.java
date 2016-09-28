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

package inr.numass.cryotemp;

/**
 * Created by darksnake on 28-Sep-16.
 */
public class PKT8Result {

    public String channel;
    public double rawValue;
    public double temperature;

    public PKT8Result(String channel, double rawValue, double temperature) {
        this.channel = channel;
        this.rawValue = rawValue;
        this.temperature = temperature;
    }

}
