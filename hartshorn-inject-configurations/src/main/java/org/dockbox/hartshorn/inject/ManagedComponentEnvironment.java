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

package org.dockbox.hartshorn.inject;

import org.dockbox.hartshorn.inject.component.ComponentRegistry;

/**
 * Represents an environment that is capable of managing components.
 *
 * @see ComponentRegistry
 *
 * @since 0.7.0
 *
 * @author Guus Lieben
 */
public interface ManagedComponentEnvironment extends InjectorEnvironment {

    /**
     * @return the {@link ComponentRegistry} that is used by this {@link ManagedComponentEnvironment} to locate components
     */
    ComponentRegistry componentRegistry();
}
