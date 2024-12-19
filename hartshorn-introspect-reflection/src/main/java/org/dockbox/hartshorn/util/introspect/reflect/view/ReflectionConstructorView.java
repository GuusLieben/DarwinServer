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

package org.dockbox.hartshorn.util.introspect.reflect.view;

import org.dockbox.hartshorn.reporting.DiagnosticsPropertyCollector;
import org.dockbox.hartshorn.reporting.Reportable;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.TypeVariablesIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionTypeVariablesIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectiveConstructorCall;
import org.dockbox.hartshorn.util.introspect.view.ConstructorView;
import org.dockbox.hartshorn.util.introspect.view.ParameterView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.option.Option;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A view that provides access to the constructor of a class. This view is backed by a {@link Constructor} instance.
 *
 * @param <T> the type of the class that the constructor belongs to
 *
 * @since 0.4.13
 *
 * @author Guus Lieben
 */
public class ReflectionConstructorView<T> extends ReflectionExecutableElementView<T> implements ConstructorView<T> {

    private final Constructor<T> constructor;
    private final Introspector introspector;

    private TypeVariablesIntrospector typeParametersIntrospector;
    private ReflectiveConstructorCall<T> invoker;
    private String qualifiedName;
    private TypeView<T> type;

    public ReflectionConstructorView(ReflectionIntrospector introspector, Constructor<T> constructor) {
        super(introspector, constructor);
        this.constructor = constructor;
        this.introspector = introspector;
    }

    protected ReflectiveConstructorCall<T> invoker() {
        if (this.invoker == null) {
            this.invoker = args -> {
                try {
                    return this.constructor.newInstance(args);
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof Exception ex) {
                        throw ex;
                    }
                    throw e;
                }
            };
        }
        return this.invoker;
    }

    @Override
    public Option<Constructor<T>> constructor() {
        return Option.of(this.constructor);
    }

    @Override
    public T create(Collection<?> arguments) throws Throwable {
        return this.invoker().invoke(arguments.toArray());
    }

    @Override
    public TypeView<T> type() {
        if (this.type == null) {
            this.type = this.introspector.introspect(this.constructor.getDeclaringClass());
        }
        return this.type;
    }

    @Override
    public TypeView<T> genericType() {
        return this.type();
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

    @Override
    public void report(DiagnosticsPropertyCollector collector) {
        collector.property("type").writeDelegate(this.type());
        collector.property("elementType").writeString("constructor");
        collector.property("parameters").writeDelegates(this.parameters().all().toArray(Reportable[]::new));
    }
}
