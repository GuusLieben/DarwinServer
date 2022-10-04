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

package org.dockbox.hartshorn.util.introspect.reflect.view;

import org.dockbox.hartshorn.util.Result;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.TypeVariablesIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionTypeVariablesIntrospector;
import org.dockbox.hartshorn.util.introspect.view.ConstructorView;
import org.dockbox.hartshorn.util.introspect.view.ParameterView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReflectionConstructorView<T> extends ReflectionExecutableElementView<T> implements ConstructorView<T> {

    private final Constructor<T> constructor;
    private final Introspector introspector;

    private TypeVariablesIntrospector typeParametersIntrospector;
    private Function<Object[], Result<T>> invoker;
    private String qualifiedName;

    public ReflectionConstructorView(final Introspector introspector, final Constructor<T> constructor) {
        super(introspector, constructor);
        this.constructor = constructor;
        this.introspector = introspector;
    }

    protected Function<Object[], Result<T>> invoker() {
        if (this.invoker == null) {
            this.invoker = args -> Result.of(() -> {
                try {
                    return this.constructor.newInstance(args);
                } catch (final InvocationTargetException e) {
                    if (e.getCause() instanceof Exception ex) throw ex;
                    throw e;
                }
            });
        }
        return this.invoker;
    }

    @Override
    public Constructor<T> constructor() {
        return this.constructor;
    }

    @Override
    public Result<T> create(final Collection<?> arguments) {
        return this.invoker().apply(arguments.toArray());
    }

    @Override
    public Result<T> createWithContext() {
        final Object[] args = this.parameters().loadFromContext();
        return this.create(args);
    }

    @Override
    public TypeView<T> type() {
        return this.introspector.introspect(this.constructor.getDeclaringClass());
    }

    @Override
    public String name() {
        return this.qualifiedName();
    }

    @Override
    public String qualifiedName() {
        if (this.qualifiedName == null) {
            this.qualifiedName = "%s(%s)".formatted(
                    this.type().qualifiedName(),
                    this.parameters().all().stream()
                            .map(ParameterView::name)
                            .collect(Collectors.joining(", "))
            );
        }
        return this.qualifiedName;
    }

    @Override
    public TypeVariablesIntrospector typeVariables() {
        if (this.typeParametersIntrospector == null) {
            this.typeParametersIntrospector = new ReflectionTypeVariablesIntrospector(this.introspector, List.of(this.constructor.getTypeParameters()));
        }
        return this.typeParametersIntrospector;
    }
}
