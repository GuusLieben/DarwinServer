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

package org.dockbox.hartshorn.beans;

import org.dockbox.hartshorn.inject.Key;
import org.dockbox.hartshorn.util.StringUtilities;
import org.dockbox.hartshorn.inject.Context;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.inject.Inject;

public class ContextBeanProvider implements BeanProvider {

    private final BeanContext beanContext;

    @Inject
    public ContextBeanProvider(@Context final BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    private Predicate<BeanReference<?>> typeFilter(final Class<?> type) {
        return ref -> ref.type().isChildOf(type);
    }

    private Predicate<BeanReference<?>> idFilter(final String id) {
        if (StringUtilities.empty(id)) return ref -> true;
        return ref -> ref.id().equals(id);
    }

    private Predicate<BeanReference<?>> typeAndIdFilter(final Class<?> type, final String id) {
        return this.typeFilter(type).and(this.idFilter(id));
    }

    @Override
    public <T> T first(final Class<T> type) {
        return this.first(type, this.typeFilter(type));
    }

    @Override
    public <T> T first(final Class<T> type, final String id) {
        return this.first(type, this.typeAndIdFilter(type, id));
    }

    @Override
    public <T> T first(final Key<T> key) {
        if (key.name() != null) return this.first(key.type(), key.name().value());
        return this.first(key.type());
    }

    private <T> T first(final Class<T> type, final Predicate<BeanReference<?>> predicate) {
        return this.stream(type, predicate)
                .findFirst()
                .orElse(null);
    }

    @Override
    public <T> List<T> all(final Class<T> type) {
        return this.stream(type, this.typeFilter(type))
                .toList();
    }

    @Override
    public <T> List<T> all(final Class<T> type, final String id) {
        return this.stream(type, this.typeAndIdFilter(type, id))
                .toList();
    }

    @Override
    public <T> List<T> all(final Key<T> key) {
        if (key.name() != null) return this.all(key.type(), key.name().value());
        else return this.all(key.type());
    }

    private <T>Stream<T> stream(final Class<T> type, final Predicate<BeanReference<?>> predicate) {
        return this.beanContext.beans().stream()
                .filter(predicate)
                .map(BeanReference::bean)
                .map(type::cast);
    }
}
