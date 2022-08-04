/*
 * Copyright 2019-2022 the original author or authors.
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

package org.dockbox.hartshorn.context;

import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.inject.Key;
import org.dockbox.hartshorn.util.Result;
import org.dockbox.hartshorn.util.reflect.TypeContext;

public interface ApplicationAwareContext extends Context, ContextCarrier {

    /**
     * Returns the first context of the given type. If it doesn't exist, but the context is annotated with
     * {@link AutoCreating}, it will be created using the provided
     * {@link ApplicationContext}.
     *
     * @param context The type of the context.
     * @param <C> The type of the context.
     * @return The first context of the given type.
     */
    @Override
    default <C extends Context> Result<C> first(final TypeContext<C> context) {
        return this.first(context.type());
    }

    /**
     * Returns the first context of the given type. If it doesn't exist, but the context is annotated with
     * {@link AutoCreating}, it will be created using the provided
     * {@link ApplicationContext}.
     *
     * @param context The type of the context.
     * @param <C> The type of the context.
     * @return The first context of the given type.
     */
    @Override
    <C extends Context> Result<C> first(Class<C> context);

    /**
     * Returns the first context of the given type and name. If it doesn't exist, but the context is annotated with
     * {@link AutoCreating}, it will be created using the provided
     * {@link ApplicationContext}.
     *
     * @param context The type of the context.
     * @param name The name of the context.
     * @param <C> The type of the context.
     * @return The first context of the given type and name.
     */
    @Override
    <C extends Context> Result<C> first(Class<C> context, String name);

    /**
     * Returns the first context of the given type and name. If it doesn't exist, but the context is annotated with
     * {@link AutoCreating}, it will be created using the provided
     * {@link ApplicationContext}.
     *
     * @param context The type of the context.
     * @param name The name of the context.
     * @param <C> The type of the context.
     * @return The first context of the given type and name.
     */
    @Override
    default <C extends Context> Result<C> first(TypeContext<C> context, String name) {
        return this.first(context.type(), name);
    }

    /**
     * Returns the first context of the given type and name, which are represented by the given key. If it doesn't exist,
     * but the context is annotated with {@link AutoCreating}, it will be
     * created using the provided {@link ApplicationContext}.
     *
     * @param context The key of the context.
     * @param <C> The type of the context.
     * @return The first context of the given type and name.
     */
    @Override
    <C extends Context> Result<C> first(Key<C> context);
}
