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

import org.dockbox.hartshorn.inject.provider.LifecycleType;
import org.dockbox.hartshorn.util.introspect.view.TypeView;

import java.util.Comparator;

/**
 * Represents a container for a component. The metadata provided by this container is used to register the component with
 * the IoC container.
 *
 * @see ComponentRegistry
 *
 * @param <T> the type of the component
 *
 * @since 0.4.1
 *
 * @author Guus Lieben
 */
public interface ComponentContainer<T> {

    Comparator<ComponentContainer<?>> COMPARE_BY_ID = Comparator.comparing(ComponentContainer::id);

    String id();

    String name();

    TypeView<T> type();

    LifecycleType lifecycle();

    boolean lazy();

    boolean permitsProxying();

    boolean permitsProcessing();
}
