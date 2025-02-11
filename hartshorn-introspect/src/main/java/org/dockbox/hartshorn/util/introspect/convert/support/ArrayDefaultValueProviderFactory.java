/*
 * Copyright 2019-2025 the original author or authors.
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

package org.dockbox.hartshorn.util.introspect.convert.support;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.dockbox.hartshorn.util.TypeUtils;
import org.dockbox.hartshorn.util.introspect.convert.ConditionalConverter;
import org.dockbox.hartshorn.util.introspect.convert.DefaultValueProvider;
import org.dockbox.hartshorn.util.introspect.convert.DefaultValueProviderFactory;

import java.lang.reflect.Array;

/**
 * Provides default values for array types.
 *
 * @since 0.7.0
 *
 * @author Guus Lieben
 */
public class ArrayDefaultValueProviderFactory implements DefaultValueProviderFactory<Object>, ConditionalConverter {

    @Override
    public <O> DefaultValueProvider<O> create(Class<O> targetType) {
        if (!targetType.isArray()) {
            throw new IllegalArgumentException("Target type must be an array type");
        }
        Class<?> elementType = targetType.getComponentType();
        ArrayDefaultValueProvider<?> provider = new ArrayDefaultValueProvider<>(elementType);
        return TypeUtils.unchecked(provider, DefaultValueProvider.class);
    }

    @Override
    public boolean canConvert(Object source, Class<?> targetType) {
        return targetType.isArray();
    }

    /**
     * Provides a default value for an array type.
     *
     * @param <O> the type of the array elements
     *
     * @see Array#newInstance(Class, int)
     *
     * @since 0.7.0
     *
     * @author Guus Lieben
     */
    public static class ArrayDefaultValueProvider<O> implements DefaultValueProvider<O[]> {

        private final Class<O> elementType;

        public ArrayDefaultValueProvider(Class<O> elementType) {
            this.elementType = elementType;
        }

        @Override
        public @Nullable O[] defaultValue() {
            //noinspection unchecked
            return (O[]) Array.newInstance(this.elementType, 0);
        }
    }
}
