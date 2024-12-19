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

package org.dockbox.hartshorn.launchpad.environment;

import org.dockbox.hartshorn.util.introspect.view.TypeView;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * A {@link EnvironmentTypeResolver} that uses an {@link EnvironmentTypeCollector} to resolve types.
 *
 * @since 0.6.0
 *
 * @author Guus Lieben
 */
public class EnvironmentTypeCollectorTypeResolver implements EnvironmentTypeResolver {

    private final EnvironmentTypeCollector typeCollector;

    public EnvironmentTypeCollectorTypeResolver(EnvironmentTypeCollector typeCollector) {
        this.typeCollector = typeCollector;
    }

    @Override
    public <A extends Annotation> Collection<TypeView<?>> types(Class<A> annotation) {
        return this.typeCollector.types(type -> type.annotations().has(annotation));
    }
}
