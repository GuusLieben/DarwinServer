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

import org.dockbox.hartshorn.context.ContextIdentity;
import org.dockbox.hartshorn.inject.ComponentKey;
import org.dockbox.hartshorn.inject.InjectionCapableApplication;
import org.dockbox.hartshorn.inject.collection.CollectionBindingHierarchy;
import org.dockbox.hartshorn.inject.provider.singleton.SingletonCache;
import org.dockbox.hartshorn.inject.scope.Scope;
import org.dockbox.hartshorn.inject.scope.ScopeModuleContext;
import org.dockbox.hartshorn.util.IllegalModificationException;
import org.dockbox.hartshorn.util.TypeUtils;
import org.dockbox.hartshorn.util.collections.HashSetMultiMap;
import org.dockbox.hartshorn.util.collections.MultiMap;
import org.dockbox.hartshorn.util.collections.UnmodifiableMultiMap;
import org.dockbox.hartshorn.util.option.Option;

/**
 * A {@link HierarchicalBinder} that is aware of the scope it is bound to. This binder will only allow
 * bindings to be added to the scope it is bound to, and will not allow bindings to be added to a different
 * scope.
 *
 * @since 0.7.0
 *
 * @author Guus Lieben
 */
public class ScopeAwareHierarchicalBinder implements HierarchicalAliasCapableBinder, NestedHierarchyLookup {

    private final InjectionCapableApplication application;
    private final SingletonCache singletonCache;
    private final Scope scope;

    private HierarchyCache hierarchyCache;

    public ScopeAwareHierarchicalBinder(InjectionCapableApplication application, SingletonCache singletonCache, Scope scope) {
        this.application = application;
        this.singletonCache = singletonCache;
        this.scope = scope;
    }

    public HierarchyCache hierarchyCache() {
        if (this.hierarchyCache == null ) {
            this.hierarchyCache = new HierarchyCache(
                    this.application.environment().configuration(),
                    this.application.defaultBinder(),
                    this
            );

        }
        return this.hierarchyCache;
    }

    protected Scope scope() {
        return this.scope;
    }

    @Override
    public <C> AliasBindingFunction<C> bind(Class<C> type) {
        // Strict, so new hierarchies are created if needed, rather than using loose lookup
        ComponentKey<C> componentKey = ComponentKey.builder(type)
                .strict(true)
                .scope(this.scope) // No explicit scope provided, so expected to use the current scope instead
                .build();
        return this.bind(componentKey);
    }

    protected Scope applicationScope() {
        return this.application.defaultProvider().scope();
    }

    @Override
    public <C> AliasBindingFunction<C> bind(ComponentKey<C> key) {
        Scope componentScope = key.scope().orNull();
        if (componentScope == null) {
            componentScope = this.applicationScope();
        }

        if (componentScope != this.scope() && componentScope != this.application.defaultProvider().scope()) {
            throw new IllegalArgumentException(
                    "Cannot bind to a different scope. Expected %s, got %s for key %s".formatted(this.scope(), componentScope, key));
        }
        BindingHierarchy<C> hierarchy = this.hierarchy(key);

        ContextIdentity<ScopeModuleContext> scopeModuleContextKey = ScopeModuleContext.createKey(() -> {
            return this.applicationScope().installableScopeType();
        });
        Option<ScopeModuleContext> scopeModuleContext = this.application.firstContext(scopeModuleContextKey);

        if (scopeModuleContext.absent() && this.scope() != this.applicationScope()) {
            throw new IllegalModificationException("Cannot add binding to non-application hierarchy without a module context");
        }

        return new HierarchyBindingFunction<>(hierarchy, this, this.singletonCache, this.scope(), scopeModuleContext.orNull());
    }

    @Override
    public <C> Binder bind(BindingHierarchy<C> hierarchy) {
        this.hierarchyCache().put(hierarchy);
        return this;
    }

    @Override
    public <T> BindingHierarchy<T> hierarchy(ComponentKey<T> key) {
        return this.hierarchy(key, false);
    }

    @Override
    public MultiMap<Scope, BindingHierarchy<?>> hierarchies() {
        MultiMap<Scope, BindingHierarchy<?>> map = new HashSetMultiMap<>();
        map.putAll(this.scope(), this.hierarchyCache().hierarchies());
        return new UnmodifiableMultiMap<>(map);
    }

    @Override
    public <T> BindingHierarchy<T> hierarchy(ComponentKey<T> key, boolean useGlobalIfAbsent) {
        // If the scope is default, it means that the binding is not explicitly scoped, so it can be
        // installed in any scope. If our active scope is the active application context, it means
        // the requested scope is not installed, so we can fall back to the application scope.
        if (!key.scope().contains(this.scope()) && this.scope() != this.applicationScope()) {
            throw new IllegalArgumentException("Cannot create a binding hierarchy for a component key with a different scope");
        }

        HierarchyCache cache = this.hierarchyCache();
        BindingHierarchy<?> hierarchy = cache.getOrComputeHierarchy(key, useGlobalIfAbsent);
        BindingHierarchy<T> adjustedHierarchy = TypeUtils.unchecked(hierarchy, BindingHierarchy.class);
        // onUpdate callback is purely so updates will still be saved even if the reference is lost
        if (adjustedHierarchy instanceof SubscribableBindingHierarchy || adjustedHierarchy instanceof CollectionBindingHierarchy<?>) {
            return adjustedHierarchy;
        }
        else {
            return new SubscribableBindingHierarchy<>(adjustedHierarchy, updated -> cache.put(key.view(), updated));
        }
    }
}
