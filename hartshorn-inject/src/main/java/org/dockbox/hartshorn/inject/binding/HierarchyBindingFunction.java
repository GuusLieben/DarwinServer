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

package org.dockbox.hartshorn.inject.binding;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.dockbox.hartshorn.inject.ComponentKey;
import org.dockbox.hartshorn.inject.IllegalScopeException;
import org.dockbox.hartshorn.inject.QualifierKey;
import org.dockbox.hartshorn.inject.collection.CollectionBindingHierarchy;
import org.dockbox.hartshorn.inject.collection.CollectorBindingFunction;
import org.dockbox.hartshorn.inject.collection.ComponentCollection;
import org.dockbox.hartshorn.inject.collection.HierarchyCollectorBindingFunction;
import org.dockbox.hartshorn.inject.provider.SimpleConstructorViewDrivenProvider;
import org.dockbox.hartshorn.inject.provider.LazySingletonInstantiationStrategy;
import org.dockbox.hartshorn.inject.provider.InstantiationStrategy;
import org.dockbox.hartshorn.inject.provider.singleton.SingletonCache;
import org.dockbox.hartshorn.inject.provider.SingletonInstantiationStrategy;
import org.dockbox.hartshorn.inject.provider.SupplierInstantiationStrategy;
import org.dockbox.hartshorn.inject.scope.Scope;
import org.dockbox.hartshorn.inject.scope.ScopeKey;
import org.dockbox.hartshorn.inject.scope.ScopeModuleContext;
import org.dockbox.hartshorn.util.Customizer;
import org.dockbox.hartshorn.util.IllegalModificationException;
import org.dockbox.hartshorn.util.function.CheckedSupplier;

/**
 * A {@link BindingFunction} that configures a {@link BindingHierarchy} for a specific key. The hierarchy is
 * provided by the owning {@link Binder}.
 *
 * @param <T> The type of the component that is bound.
 *
 * @see Binder
 * @see BindingHierarchy
 *
 * @since 0.4.11
 *
 * @author Guus Lieben
 */
public class HierarchyBindingFunction<T> implements AliasBindingFunction<T> {

    private final BindingHierarchy<T> hierarchy;
    private final HierarchicalBinder binder;
    private final SingletonCache singletonCache;
    private final ScopeModuleContext moduleContext;

    private Scope scope;
    private ScopeKey scopeKey;

    private int priority = -1;
    private boolean processAfterInitialization = true;

    public HierarchyBindingFunction(
            BindingHierarchy<T> hierarchy,
            HierarchicalBinder binder,
            SingletonCache singletonCache,
            Scope scope,
            ScopeModuleContext moduleContext) {
        this.hierarchy = hierarchy;
        this.binder = binder;
        this.singletonCache = singletonCache;

        this.scope = scope;
        this.moduleContext = moduleContext;
    }

    protected BindingHierarchy<T> hierarchy() {
        if (this.scopeKey != null && !moduleContext.isApplicationScope(scopeKey)) {
            return this.moduleContext.hierarchy(this.scopeKey, this.hierarchy.key());
        }
        else {
            return this.hierarchy;
        }
    }

    protected Binder binder() {
        return this.binder;
    }

    protected SingletonCache singletonCache() {
        return this.singletonCache;
    }

    @Override
    public AliasBindingFunction<T> alias(Class<? super T> aliasType) {
        throw new UnsupportedOperationException("Not yet implemented"); // TODO: Implement
    }

    @Override
    public AliasBindingFunction<T> alias(ComponentKey<? super T> aliasKey) {
        throw new UnsupportedOperationException("Not yet implemented"); // TODO: Implement
    }

    @Override
    public AliasBindingFunction<T> alias(QualifierKey<T> aliasQualifier) {
        throw new UnsupportedOperationException("Not yet implemented"); // TODO: Implement
    }

    @Override
    public AliasBindingFunction<T> installTo(ScopeKey scopeKey) throws IllegalScopeException {
        boolean expandingApplicationScope = this.moduleContext.isApplicationScope(this.scopeKey)
                || this.moduleContext.isApplicationScope(this.scope.installableScopeType());

        if (!expandingApplicationScope) {
            throw new IllegalScopeException("Cannot install binding to child scope " + scopeKey.name() + " as the binding is already installed to child scope " + this.scope.installableScopeType().name());
        }
        // Permitted, as default application scope may be expanded. Defined child scopes can not be expanded, so this is a safe check
        if (!this.moduleContext.isApplicationScope(scopeKey)) {
            this.scope = null;
        }
        this.scopeKey = scopeKey;
        return this;
    }

    @Override
    public AliasBindingFunction<T> priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public AliasBindingFunction<T> processAfterInitialization(boolean processAfterInitialization) {
        this.processAfterInitialization = processAfterInitialization;
        return this;
    }

    @Override
    public Binder to(Class<? extends T> type) {
        if (this.singletonCache.contains(this.hierarchy().key())) {
            throw new IllegalModificationException("Cannot overwrite singleton binding for %s in a hierarchy, ensure the new binding is a singleton".formatted(this.hierarchy().key()));
        }
        ComponentKey<? extends T> key = this.buildComponentKey(type);
        return this.add(SimpleConstructorViewDrivenProvider.forPrototype(key));
    }

    @NonNull
    private ComponentKey<? extends T> buildComponentKey(Class<? extends T> type) {
        return ComponentKey.builder(type).scope(this.scope).build();
    }

    @Override
    public Binder to(CheckedSupplier<T> supplier) {
        if (this.singletonCache.contains(this.hierarchy().key())) {
            throw new IllegalModificationException("Cannot overwrite singleton binding for %s in a hierarchy, ensure the new binding is a singleton".formatted(this.hierarchy().key()));
        }
        return this.add(new SupplierInstantiationStrategy<>(supplier));
    }

    @Override
    public Binder to(InstantiationStrategy<T> strategy) {
        return this.add(strategy);
    }

    @Override
    public Binder singleton(T instance) {
        if (instance == null) {
            throw new IllegalModificationException("Cannot bind null instance");
        }
        if (this.processAfterInitialization) {
            return this.add(new SingletonInstantiationStrategy<>(instance));
        }
        else {
            // If no processing should happen, then we can immediately cache the instance
            this.singletonCache.put(this.hierarchy.key(), instance);
            return this.binder();
        }
    }

    @Override
    public Binder lazySingleton(Class<T> type) {
        ComponentKey<? extends T> key = this.buildComponentKey(type);
        return this.add(SimpleConstructorViewDrivenProvider.forSingleton(key));
    }

    @Override
    public Binder lazySingleton(CheckedSupplier<T> supplier) {
        return this.add(new LazySingletonInstantiationStrategy<>(supplier));
    }

    @Override
    public Binder collect(Customizer<CollectorBindingFunction<T>> collector) {
        BindingHierarchy<T> existingHierarchy = this.hierarchy();
        ComponentKey<ComponentCollection<T>> collectionComponentKey = this.createCollectionComponentKey();

        BindingHierarchy<ComponentCollection<T>> existingCollectionHierarchy = this.binder.hierarchy(collectionComponentKey);
        if (existingCollectionHierarchy instanceof CollectionBindingHierarchy<T> collectionBindingHierarchy) {
            Binder updatedBinder = this.binder.bind(collectionBindingHierarchy);
            CollectorBindingFunction<T> function = new HierarchyCollectorBindingFunction<>(updatedBinder, collectionBindingHierarchy, this.priority);
            collector.configure(function);
            return updatedBinder;
        }
        else {
            throw new IllegalStateException("Cannot create collector binding function for hierarchy " + existingHierarchy.key() + " as it is not a collection binding hierarchy");
        }
    }

    private ComponentKey<ComponentCollection<T>> createCollectionComponentKey() {
        return this.hierarchy().key().mutable().collector().build();
    }

    protected Binder add(InstantiationStrategy<T> strategy) {
        strategy = strategy.map(container -> {
            container.processed(!this.processAfterInitialization);
            return container;
        });
        this.hierarchy().add(this.priority, strategy);
        return this.binder();
    }
}
