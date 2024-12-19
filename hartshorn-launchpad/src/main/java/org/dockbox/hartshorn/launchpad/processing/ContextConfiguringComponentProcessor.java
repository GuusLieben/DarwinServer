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

package org.dockbox.hartshorn.launchpad.processing;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.dockbox.hartshorn.context.Context;
import org.dockbox.hartshorn.context.ContextView;
import org.dockbox.hartshorn.inject.ComponentKey;
import org.dockbox.hartshorn.inject.ContextKey;
import org.dockbox.hartshorn.inject.InjectionCapableApplication;
import org.dockbox.hartshorn.inject.processing.ComponentPostProcessor;
import org.dockbox.hartshorn.inject.processing.ComponentProcessingContext;
import org.dockbox.hartshorn.proxy.ProxyFactory;
import org.dockbox.hartshorn.util.TypeUtils;

/**
 * A {@link ComponentPostProcessor} that configures components with a specific context. This context is created by the
 * implementation of this class. Depending on how the component is being created, the context may be added directly to
 * the component, or to the proxy that is created for the component.
 *
 * @param <C> The type of the context that is being configured
 *
 * @since 0.5.0
 *
 * @author Guus Lieben
 */
public abstract class ContextConfiguringComponentProcessor<C extends ContextView> extends ComponentPostProcessor {

    private final Class<C> contextType;

    protected ContextConfiguringComponentProcessor(Class<C> contextType) {
        this.contextType = contextType;
    }

    @Override
    public <T> void preConfigureComponent(InjectionCapableApplication application, @Nullable T instance, ComponentProcessingContext<T> processingContext) {
        if (this.supports(processingContext)) {
            C componentContext = processingContext.firstContext(ContextKey.of(this.contextType))
                    .orCompute(() -> this.createContext(application, processingContext)).orNull();

            if (componentContext != null) {
                this.configure(application, componentContext, processingContext);
            }

            if (instance instanceof Context contextInstance) {
                contextInstance.addContext(componentContext);
            }
            else {
                ComponentKey<ProxyFactory<T>> factoryKey = TypeUtils.unchecked(ComponentKey.of(ProxyFactory.class), ComponentKey.class);
                if (processingContext.containsKey(factoryKey)) {
                    ProxyFactory<T> proxyFactory = processingContext.get(factoryKey);
                    proxyFactory.contextContainer().addContext(componentContext);
                }
            }
        }
    }

    protected abstract boolean supports(
        ComponentProcessingContext<?> processingContext
    );

    protected abstract <T> void configure(
        InjectionCapableApplication application,
        C componentContext,
        ComponentProcessingContext<T> processingContext
    );

    protected abstract C createContext(
        InjectionCapableApplication application,
        ComponentProcessingContext<?> processingContext
    );
}
