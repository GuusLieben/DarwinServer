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

import org.dockbox.hartshorn.util.introspect.ElementModifiersIntrospector;
import org.dockbox.hartshorn.util.introspect.ExecutableParametersIntrospector;
import org.dockbox.hartshorn.util.introspect.IllegalIntrospectionException;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.TypeVariablesIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionElementModifiersIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionExecutableParametersIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionTypeVariablesIntrospector;
import org.dockbox.hartshorn.util.introspect.view.EnclosableView;
import org.dockbox.hartshorn.util.introspect.view.ExecutableElementView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.option.Option;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.util.List;

/**
 * A view that provides access to the constructor of a class. This view is backed by a {@link Executable} instance.
 *
 * @see ExecutableParametersIntrospector
 * @see TypeVariablesIntrospector
 *
 * @param <Parent> the type of the class that the constructor belongs to
 *
 * @since 0.4.13
 *
 * @author Guus Lieben
 */
public abstract class ReflectionExecutableElementView<Parent> extends ReflectionAnnotatedElementView implements ExecutableElementView<Parent> {

    private final Introspector introspector;
    private final Executable executable;

    private ExecutableParametersIntrospector parametersIntrospector;
    private TypeVariablesIntrospector typeVariablesIntrospector;

    private TypeView<Parent> declaredBy;

    protected ReflectionExecutableElementView(ReflectionIntrospector introspector, Executable executable) {
        super(introspector);
        this.executable = executable;
        this.introspector = introspector;
        if (!executable.trySetAccessible()) {
            String packageName = executable.getDeclaringClass().getPackageName();
            if (!(packageName.startsWith("java.") || packageName.startsWith("jdk.") || packageName.startsWith("sun.") || packageName.startsWith("com.sun.") || packageName.startsWith("javax."))) {
                throw new IllegalIntrospectionException(executable, "Unable to set executable " + executable.getName() + " accessible");
            }
        }
    }

    /**
     * Returns the underlying {@link Executable} instance.
     *
     * @return the underlying {@link Executable} instance
     */
    public Executable executable() {
        return this.executable;
    }

    @Override
    public ExecutableParametersIntrospector parameters() {
        if (this.parametersIntrospector == null) {
            this.parametersIntrospector = new ReflectionExecutableParametersIntrospector(this.introspector, this);
        }
        return this.parametersIntrospector;
    }

    @Override
    public TypeVariablesIntrospector typeVariables() {
        if (this.typeVariablesIntrospector == null) {
            this.typeVariablesIntrospector = new ReflectionTypeVariablesIntrospector(this.introspector, List.of(this.executable.getTypeParameters()));
        }
        return this.typeVariablesIntrospector;
    }

    @Override
    public TypeView<Parent> declaredBy() {
        if (this.declaredBy == null) {
            this.declaredBy = (TypeView<Parent>) this.introspector.introspect(this.executable.getDeclaringClass());
        }
        return this.declaredBy;
    }

    @Override
    protected AnnotatedElement annotatedElement() {
        return this.executable;
    }

    @Override
    public ElementModifiersIntrospector modifiers() {
        return new ReflectionElementModifiersIntrospector(this.executable);
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
