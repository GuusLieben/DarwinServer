/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dockbox.hartshorn.inject.populate;

/**
 * Interface that defines a component populator. A component populator is used to populate injection points of
 * a component after it has been instantiated. This allows for additional configuration of components after they
 * have been created.
 *
 * @since 0.4.1
 *
 * @author Guus Lieben
 */
public interface ComponentPopulator {

    /**
     * Populates the provided instance with additional configuration. This method is called after the instance has been
     * created and before it is returned to the caller.
     *
     * @param instance the instance to populate
     * @return the populated instance
     * @param <T> the type of the instance
     */
    <T> T populate(T instance);
}
