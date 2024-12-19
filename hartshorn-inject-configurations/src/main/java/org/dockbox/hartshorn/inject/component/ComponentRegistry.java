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

package org.dockbox.hartshorn.inject.component;

import org.dockbox.hartshorn.util.option.Option;

import java.util.Collection;

/**
 * Registry for components. This registry is used to store and retrieve components that are registered with the IoC
 * container.
 *
 * @since 0.4.1
 *
 * @author Guus Lieben
 */
public interface ComponentRegistry {

    /**
     * Obtains all registered component containers. If no components are registered, an empty collection is returned.
     *
     * @return all registered component containers
     */
    Collection<ComponentContainer<?>> containers();

    /**
     * Obtains a component container for the provided type. If no component is registered for the provided type, an
     * empty option is returned.
     *
     * @param type the type of the component
     * @return a component container for the provided type
     */
    Option<ComponentContainer<?>> container(Class<?> type);
}
