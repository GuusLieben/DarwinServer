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
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionIntrospector;
import org.dockbox.hartshorn.util.introspect.view.EnclosableView;
import org.dockbox.hartshorn.util.introspect.view.ExecutableElementView;
import org.dockbox.hartshorn.util.introspect.view.ParameterView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.option.Option;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * A view that provides access to the parameters of a method or constructor. This view is backed by a {@link Parameter}
 * instance.
 *
 * @param <T> the type of the parameter
 *
 * @since 0.4.13
 *
 * @author Guus Lieben
 */
public class ReflectionParameterView<T> extends ReflectionAnnotatedElementView implements ParameterView<T> {

    private final Introspector introspector;
    private final Parameter parameter;
    private String name;
    private ExecutableElementView<?> declaredBy;
    private TypeView<T> type;
    private TypeView<T> genericType;

    public ReflectionParameterView(ReflectionIntrospector introspector, Parameter parameter) {
        super(introspector);
        this.introspector = introspector;
        this.parameter = parameter;
    }

    @Override
    public TypeView<T> type() {
        if (this.type == null) {
            this.type = (TypeView<T>) this.introspector.introspect(this.parameter.getType());
        }
        return this.type;
    }

    @Override
    public TypeView<T> genericType() {
        if (this.genericType == null) {
            this.genericType = (TypeView<T>) this.introspector.introspect(this.parameter.getParameterizedType());
        }
        return this.genericType;
    }

    @Override
    public String name() {
        if (this.name == null) {
            if (this.annotations().has(org.dockbox.hartshorn.util.introspect.Parameter.class)) {
                this.name = this.annotations().get(org.dockbox.hartshorn.util.introspect.Parameter.class).get().value();
            } else {
                this.name = this.parameter.getName();
            }
        }
        return this.name;
    }

    @Override
    public String qualifiedName() {
        String executableName = this.declaredBy().qualifiedName();
        return executableName + "[" + this.name() + "]";
    }

    @Override
    public boolean isVarArgs() {
        return this.parameter.isVarArgs();
    }

    @Override
    public boolean isNamePresent() {
        return this.parameter.isNamePresent();
    }

    @Override
    public ExecutableElementView<?> declaredBy() {
        if (this.declaredBy == null) {
            Executable executable = this.parameter.getDeclaringExecutable();
            if (executable instanceof Method method) {
                this.declaredBy = this.introspector.introspect(method);
            }
            else if (executable instanceof Constructor<?> constructor) {
                this.declaredBy = this.introspector.introspect(constructor);
            }
            else {
                throw new RuntimeException("Unexpected executable type: " + executable.getName());
            }
        }
        return this.declaredBy;
    }

    @Override
    protected AnnotatedElement annotatedElement() {
        return this.parameter;
    }

    @Override
    public void report(DiagnosticsPropertyCollector collector) {
        collector.property("name").writeString(this.name());
        collector.property("type").writeDelegate(this.genericType());
    }

    @Override
    public boolean isEnclosed() {
        return true;
    }

    @Override
    public Option<EnclosableView> enclosingView() {
        return Option.of(this.declaredBy());
    }
}
