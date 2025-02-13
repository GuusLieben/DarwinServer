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

package org.dockbox.hartshorn.launchpad;

import org.dockbox.hartshorn.inject.ComponentKey;
import org.dockbox.hartshorn.inject.IllegalScopeException;
import org.dockbox.hartshorn.inject.QualifierKey;
import org.dockbox.hartshorn.inject.binding.AliasBindingFunction;
import org.dockbox.hartshorn.inject.binding.Binder;
import org.dockbox.hartshorn.inject.binding.BindingFunction;
import org.dockbox.hartshorn.inject.scope.ScopeKey;

/**
 * A {@link BindingFunction} that delegates all calls to the provided {@link BindingFunction delegate}, but returns the
 * {@link ApplicationContext} instead of the {@link Binder} to allow for chaining. This is used to allow for the
 * {@link ApplicationContext} to use custom binders, while still allowing for the {@link ApplicationContext} to be
 * returned.
 *
 * @param <T> the type of the binding
 *
 * @see ApplicationContext
 * @see BindingFunction
 *
 * @since 0.4.11
 *
 * @author Guus Lieben
 */
public class DelegatingApplicationAliasBindingFunction<T> extends DelegatingApplicationBindingFunction<T> implements AliasBindingFunction<T> {

    public DelegatingApplicationAliasBindingFunction(ApplicationContext applicationContext, AliasBindingFunction<T> delegate) {
        super(applicationContext, delegate);
    }

    @Override
    protected AliasBindingFunction<T> delegate() {
        return (AliasBindingFunction<T>) super.delegate();
    }

    @Override
    public AliasBindingFunction<T> alias(Class<? super T> aliasType) {
        return this.delegate().alias(aliasType);
    }

    @Override
    public AliasBindingFunction<T> alias(ComponentKey<? super T> aliasKey) {
        return this.delegate().alias(aliasKey);
    }

    @Override
    public AliasBindingFunction<T> alias(QualifierKey<T> aliasQualifier) {
        return this.delegate().alias(aliasQualifier);
    }

    @Override
    public AliasBindingFunction<T> installTo(ScopeKey scope) throws IllegalScopeException {
        return (AliasBindingFunction<T>) super.installTo(scope);
    }

    @Override
    public AliasBindingFunction<T> priority(int priority) {
        return (AliasBindingFunction<T>) super.priority(priority);
    }

    @Override
    public AliasBindingFunction<T> processAfterInitialization(boolean processAfterInitialization) {
        return (AliasBindingFunction<T>) super.processAfterInitialization(processAfterInitialization);
    }
}
