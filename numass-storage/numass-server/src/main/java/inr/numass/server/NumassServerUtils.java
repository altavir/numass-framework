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

package inr.numass.server;

import hep.dataforge.exceptions.StorageException;
import hep.dataforge.storage.api.ObjectLoader;

import java.util.stream.Stream;

/**
 * Created by darksnake on 02-Oct-16.
 */
public class NumassServerUtils {
    /**
     * Stream of notes in the last to first order
     *
     * @return
     */
    public static Stream<NumassNote> getNotes(ObjectLoader<NumassNote> noteLoader) {
        return noteLoader.fragmentNames().stream().map(name -> {
            try {
                return noteLoader.pull(name);
            } catch (StorageException ex) {
                return null;
            }
        }).sorted((o1, o2) -> -o1.time().compareTo(o2.time()));
    }
}
