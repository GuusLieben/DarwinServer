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

import org.dockbox.hartshorn.inject.IllegalScopeException;
import org.dockbox.hartshorn.inject.binding.Binder;
import org.dockbox.hartshorn.inject.binding.BindingFunction;
import org.dockbox.hartshorn.inject.collection.CollectorBindingFunction;
import org.dockbox.hartshorn.inject.provider.InstantiationStrategy;
import org.dockbox.hartshorn.inject.scope.ScopeKey;
import org.dockbox.hartshorn.launchpad.context.ApplicationContextCarrier;
import org.dockbox.hartshorn.util.Customizer;
import org.dockbox.hartshorn.util.function.CheckedSupplier;

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
public class DelegatingApplicationBindingFunction<T> implements BindingFunction<T>, ApplicationContextCarrier {

    private final ApplicationContext applicationContext;
    private final BindingFunction<T> delegate;

    public DelegatingApplicationBindingFunction(ApplicationContext applicationContext, BindingFunction<T> delegate) {
        this.applicationContext = applicationContext;
        this.delegate = delegate;
    }

    protected BindingFunction<T> delegate() {
        return this.delegate;
    }

    @Override
    public BindingFunction<T> installTo(ScopeKey scope) throws IllegalScopeException {
        return this.delegate.installTo(scope);
    }

    @Override
    public BindingFunction<T> priority(int priority) {
        return this.delegate.priority(priority);
    }

    @Override
    public BindingFunction<T> processAfterInitialization(boolean processAfterInitialization) {
        return this.delegate.processAfterInitialization(processAfterInitialization);
    }

    @Override
    public ApplicationContext to(Class<? extends T> type) {
        this.delegate.to(type);
        return this.applicationContext;
    }

    @Override
    public ApplicationContext to(CheckedSupplier<T> supplier) {
        this.delegate.to(supplier);
        return this.applicationContext;
    }

    @Override
    public Binder to(InstantiationStrategy<T> strategy) {
        return this.delegate.to(strategy);
    }

    @Override
    public ApplicationContext singleton(T instance) {
        this.delegate.singleton(instance);
        return this.applicationContext;
    }

    @Override
    public ApplicationContext lazySingleton(Class<T> type) {
        this.delegate.lazySingleton(type);
        return this.applicationContext;
    }

    @Override
    public ApplicationContext lazySingleton(CheckedSupplier<T> supplier) {
        this.delegate.lazySingleton(supplier);
        return this.applicationContext;
    }

    @Override
    public ApplicationContext applicationContext() {
        return this.applicationContext;
    }

    @Override
    public Binder collect(Customizer<CollectorBindingFunction<T>> collector) {
        return this.delegate.collect(collector);
    }
}
