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
 * Resolver to find annotated types in the environment. Depending on the environment and implementation of
 * this interface, this may also support meta-annotations or annotation hierarchies (typically through the
 * use of {@link org.dockbox.hartshorn.util.introspect.annotations.Extends} decorators).
 *
 * @since 0.6.0
 *
 * @author Guus Lieben
 */
public interface EnvironmentTypeResolver {

    /**
     * Gets types decorated with a given annotation, both classes and annotations.
     *
     * @param <A> The annotation constraint
     * @param annotation The annotation expected to be present on one or more types
     * @return The annotated types
     */
    <A extends Annotation> Collection<TypeView<?>> types(Class<A> annotation);

}
