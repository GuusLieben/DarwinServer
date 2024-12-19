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

package org.dockbox.hartshorn.inject.graph;

import org.dockbox.hartshorn.inject.ComponentKey;
import org.dockbox.hartshorn.inject.component.ComponentContainer;
import org.dockbox.hartshorn.inject.provider.LifecycleType;
import org.dockbox.hartshorn.util.introspect.view.ConstructorView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;

/**
 * Represents a dependency context for a {@link ComponentContainer}. This context is used to determine the lifecycle and
 * other properties of a component that is managed by a container.
 *
 * @param <T> the type of the component
 *
 * @since 0.6.0
 *
 * @author Guus Lieben
 */
public class ComponentContainerDependencyContext<T> extends ManagedComponentDependencyContext<T> {

    private final ComponentContainer<T> container;

    public ComponentContainerDependencyContext(ComponentContainer<T> container,
        ComponentKey<T> componentKey, DependencyMap dependencies,
        ConstructorView<? extends T> constructorView) {
        super(componentKey, dependencies, constructorView);
        this.container = container;
    }

    @Override
    public boolean lazy() {
        return this.container == null || this.container.lazy();
    }

    @Override
    public LifecycleType lifecycleType() {
        return this.container != null ? this.container.lifecycle() : LifecycleType.PROTOTYPE;
    }

    @Override
    public boolean processAfterInitialization() {
        return this.container == null || this.container.permitsProcessing();
    }

    @Override
    protected TypeView<T> type() {
        return this.container.type();
    }
}
