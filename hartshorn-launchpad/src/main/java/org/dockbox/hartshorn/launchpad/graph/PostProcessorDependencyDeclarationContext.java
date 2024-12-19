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

package org.dockbox.hartshorn.launchpad.graph;

import org.dockbox.hartshorn.inject.CompositeQualifier;
import org.dockbox.hartshorn.inject.graph.declaration.DependencyDeclarationContext;
import org.dockbox.hartshorn.inject.processing.ComponentPostProcessor;
import org.dockbox.hartshorn.util.introspect.view.TypeView;

/**
 * A {@link DependencyDeclarationContext} that is used to declare a {@link ComponentPostProcessor} as a
 * component within the IoC container.
 *
 * @param <T> The type of the post processor that is being declared
 * @param type The type of the post processor that is being declared
 *
 * @since 0.5.0
 *
 * @author Guus Lieben
 */
public record PostProcessorDependencyDeclarationContext<T extends ComponentPostProcessor>(
        TypeView<T> type
) implements DependencyDeclarationContext<T> {

    @Override
    public CompositeQualifier qualifier() {
        // Post processors are not qualified in any way, so we can return an empty qualifier here
        return new CompositeQualifier();
    }
}
