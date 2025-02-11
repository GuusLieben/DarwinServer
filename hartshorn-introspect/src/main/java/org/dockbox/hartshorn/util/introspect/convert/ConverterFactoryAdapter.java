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

package org.dockbox.hartshorn.util.introspect.convert;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * Adapts a {@link ConverterFactory} to a {@link GenericConverter}. This is useful when a
 * {@link ConverterFactory} needs to be used in a place where a {@link GenericConverter} is
 * expected, such as in a {@link ConverterCache}.
 *
 * <p>This converter is conditional and only matches if the {@link #convertibleTypes() convertible types}
 * match the source and target types. The target type matches if it is assignable to the target type
 * of the factory.
 *
 * <p>If the {@link ConverterFactory} instance also implements {@link ConditionalConverter}, then the
 * {@link ConditionalConverter#canConvert(Object, Class)} method is used to further narrow the match. If
 * the {@link Converter} instance created by the factory also implements {@link ConditionalConverter},
 * then the {@link ConditionalConverter#canConvert(Object, Class)} method is again used to further narrow
 * the match.
 *
 * @since 0.5.0
 *
 * @author Guus Lieben
 */
public class ConverterFactoryAdapter implements GenericConverter, ConditionalConverter {

    private final ConverterFactory<Object, Object> converterFactory;
    private final ConvertibleTypePair typePair;

    public <I, O> ConverterFactoryAdapter(Class<I> sourceType, Class<O> targetType, ConverterFactory<I, O> converterFactory) {
        this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
        this.typePair = ConvertibleTypePair.of(sourceType, targetType);
    }

    /**
     * Returns the underlying {@link ConvertibleTypePair} that this converter can convert between. This
     * represents the higher-level target type, not the source-to-target type actually handled by the
     * underlying {@link ConverterFactory}. For example, for a {@link ConverterFactory} which handles
     * {@link String} to {@link Number} conversions, this method would return {@link String} to
     * {@link Number}, but never {@link String} to {@link Integer}.
     *
     * @return the convertible type pair
     */
    public ConvertibleTypePair typePair() {
        return this.typePair;
    }

    @Override
    public Set<ConvertibleTypePair> convertibleTypes() {
        return null;
    }

    @Override
    public boolean canConvert(Object source, Class<?> targetType) {
        boolean matches = true;
        if (!this.typePair.sourceType().isAssignableFrom(source.getClass())) {
            return false;
        }
        else if (!this.typePair.targetType().isAssignableFrom(targetType)) {
            // If a factory declares a primitive target, it should only match if the target is declared as a generic Object.
            matches = targetType.isPrimitive() && this.typePair.targetType().equals(Object.class);
        }

        if (matches) {
            if (this.converterFactory instanceof ConditionalConverter conditionalConverter && !conditionalConverter.canConvert(source, targetType)) {
                return false;
            }

            Converter<?, ?> converter = this.converterFactory.create(targetType);
            if (converter instanceof ConditionalConverter conditionalConverter && !conditionalConverter.canConvert(source, targetType)) {
                return false;
            }
        }
        return matches;
    }

    @Override
    public @Nullable <I, O> Object convert(@Nullable Object source, @NonNull Class<I> sourceType, @NonNull Class<O> targetType) {
        return this.converterFactory.create(targetType).convert(sourceType.cast(source));
    }
}
