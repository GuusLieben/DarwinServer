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

import java.util.Set;
import java.util.function.BiConsumer;
import org.dockbox.hartshorn.inject.ComponentKey;
import org.dockbox.hartshorn.inject.ComponentRequestContext;
import org.dockbox.hartshorn.inject.DefaultFallbackCompatibleContext;
import org.dockbox.hartshorn.inject.ExceptionHandler;
import org.dockbox.hartshorn.inject.binding.AliasBindingFunction;
import org.dockbox.hartshorn.inject.binding.AliasCapableBinder;
import org.dockbox.hartshorn.inject.binding.Binder;
import org.dockbox.hartshorn.inject.binding.BindingFunction;
import org.dockbox.hartshorn.inject.binding.BindingHierarchy;
import org.dockbox.hartshorn.inject.binding.DefaultBindingConfigurer;
import org.dockbox.hartshorn.inject.binding.HierarchicalBinder;
import org.dockbox.hartshorn.inject.component.ComponentRegistry;
import org.dockbox.hartshorn.inject.provider.ComponentProvider;
import org.dockbox.hartshorn.inject.provider.ComponentProviderOrchestrator;
import org.dockbox.hartshorn.inject.provider.HierarchicalComponentProvider;
import org.dockbox.hartshorn.inject.provider.HierarchicalComponentProviderOrchestrator;
import org.dockbox.hartshorn.inject.provider.PostProcessingComponentProvider;
import org.dockbox.hartshorn.inject.scope.Scope;
import org.dockbox.hartshorn.launchpad.activation.ActivatorHolder;
import org.dockbox.hartshorn.launchpad.activation.ContextActivatorHolder;
import org.dockbox.hartshorn.launchpad.activation.ServiceActivatorContext;
import org.dockbox.hartshorn.launchpad.configuration.BindingConfigurerBinderPostProcessorAdapter;
import org.dockbox.hartshorn.launchpad.context.ModifiableApplicationContextCarrier;
import org.dockbox.hartshorn.launchpad.environment.ApplicationEnvironment;
import org.dockbox.hartshorn.launchpad.lifecycle.LifecycleObserver;
import org.dockbox.hartshorn.launchpad.lifecycle.ObservableApplicationEnvironment;
import org.dockbox.hartshorn.util.ContextualInitializer;
import org.dockbox.hartshorn.util.Customizer;
import org.dockbox.hartshorn.util.IllegalModificationException;
import org.dockbox.hartshorn.util.SingleElementContext;
import org.dockbox.hartshorn.util.collections.ArrayListMultiMap;
import org.dockbox.hartshorn.util.collections.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ApplicationContext} implementation that delegates to a {@link PostProcessingComponentProvider}. This
 * implementation is used to allow for custom implementations of provision and binding, while still allowing for
 * the {@link ApplicationContext} to function in a predictable manner.
 *
 * <p>Details like component processors and context initialization are not handled as they are specific to the
 * {@link ApplicationContext} implementation. This implementation is intended to be used as a base class for
 * {@link ApplicationContext} implementations.
 *
 * <p>While the lifecycle during startup is not explicitly managed by this implementation, it does provide handling for
 * {@link LifecycleObserver#onExit(ApplicationContext)}. This allows for the {@link ApplicationContext} to be closed
 * in a predictable manner.
 *
 * <p>Additional default bindings may be configured using a {@link DefaultBindingConfigurer}. This configuration will
 * be activated only for the global {@link HierarchicalBinder}, and not for any scoped binders.
 *
 * @see ApplicationContext
 * @see ComponentRegistry
 * @see ComponentProvider
 * @see ApplicationEnvironment
 * @see DelegatingApplicationContext.Configurer
 *
 * @since 0.4.11
 *
 * @author Guus Lieben
 */
public abstract class DelegatingApplicationContext
        extends DefaultFallbackCompatibleContext
        implements ProcessableApplicationContext {

    private static final Logger LOG = LoggerFactory.getLogger(DelegatingApplicationContext.class);

    private final transient ComponentProviderOrchestrator componentProvider;
    private final transient ApplicationEnvironment environment;

    private boolean isClosed = false;
    protected boolean isRunning = false;

    protected DelegatingApplicationContext(SingleElementContext<? extends ApplicationEnvironment> initializerContext, Configurer configurer) {
        this.environment = initializerContext.input();

        if (this.environment instanceof ModifiableApplicationContextCarrier modifiableApplicationContextCarrier) {
            modifiableApplicationContextCarrier.applicationContext(this);
        }

        this.prepareInitialization();

        SingleElementContext<ApplicationContext> applicationInitializerContext = initializerContext.transform(this);
        // Expose current context to allow initializers to resolve the application, even if the content of the element context
        // is not the application itself
        applicationInitializerContext.addContext(this);

        this.componentProvider = configurer.componentProvider.initialize(applicationInitializerContext.transform(this.environment().componentRegistry()));

        DefaultBindingConfigurer bindingConfigurer = configurer.defaultBindings.initialize(applicationInitializerContext);
        this.componentProvider.binderProcessorRegistry().register(new BindingConfigurerBinderPostProcessorAdapter(bindingConfigurer));
    }

    /**
     * Prepares the initialization of the {@link ApplicationContext}. This method is called before any bindings are
     * configured. This method is intended to be overridden by implementations to perform any initialization that is
     * required before bindings are configured.
     */
    protected abstract void prepareInitialization();

    /**
     * Checks if the {@link ApplicationContext} is running. If it is, an {@link IllegalModificationException} is thrown.
     * This method is intended to be called by implementations to prevent modifications to the {@link ApplicationContext}
     * after it has been started.
     */
    protected void checkRunning() {
        if (this.isRunning) {
            throw new IllegalModificationException("Application context cannot be modified after it has been started");
        }
    }

    @Override
    public Scope scope() {
        return this.defaultProvider().scope();
    }

    @Override
    public ComponentProviderOrchestrator defaultProvider() {
        return this.componentProvider;
    }

    @Override
    public HierarchicalBinder defaultBinder() {
        return this;
    }

    @Override
    public ActivatorHolder activators() {
        return ContextActivatorHolder.of(() -> this.firstContext(ServiceActivatorContext.class));
    }

    @Override
    public void handle(Throwable throwable) {
        this.environment().handle(throwable);
    }

    @Override
    public void handle(String message, Throwable throwable) {
        this.environment().handle(message, throwable);
    }

    @Override
    public ExceptionHandler printStackTraces(boolean stacktraces) {
        return this.environment().printStackTraces(stacktraces);
    }

    @Override
    public <T> T get(ComponentKey<T> key, ComponentRequestContext requestContext) {
        return this.componentProvider.get(key, requestContext);
    }

    @Override
    public <C> AliasBindingFunction<C> bind(ComponentKey<C> key) {
        if (this.componentProvider instanceof AliasCapableBinder binder) {
            AliasBindingFunction<C> function = binder.bind(key);
            return new DelegatingApplicationAliasBindingFunction<>(this, function);
        }
        else if (this.componentProvider instanceof Binder binder) {
            BindingFunction<C> function = binder.bind(key);
            BindingFunction<C> delegate = new DelegatingApplicationBindingFunction<>(this, function);
            return new NonAliasBindingFunctionAdapter<>(delegate);
        }
        throw new UnsupportedOperationException("This application does not support binding hierarchies");
    }

    @Override
    public <C> Binder bind(BindingHierarchy<C> hierarchy) {
        if (this.componentProvider instanceof Binder binder) {
            return binder.bind(hierarchy);
        }
        throw new UnsupportedOperationException("This application does not support binding hierarchies");
    }

    @Override
    public ApplicationEnvironment environment() {
        return this.environment;
    }

    @Override
    public <T> BindingHierarchy<T> hierarchy(ComponentKey<T> key) {
        if (this.componentProvider instanceof HierarchicalComponentProvider provider) {
            return provider.hierarchy(key);
        }
        throw new UnsupportedOperationException("This application does not support binding hierarchies");
    }

    @Override
    public MultiMap<Scope, BindingHierarchy<?>> hierarchies() {
        if (this.componentProvider instanceof HierarchicalComponentProvider provider) {
            return provider.hierarchies();
        }
        return new ArrayListMultiMap<>();
    }

    @Override
    public void close() {
        if (this.isClosed()) {
            throw new ContextClosedException(ApplicationContext.class);
        }
        ApplicationEnvironment environment = this.environment();
        if (environment instanceof ObservableApplicationEnvironment observable) {
            Set<LifecycleObserver> observers = observable.observers(LifecycleObserver.class);
            LOG.info("Runtime shutting down, notifying {} observers", observers.size());
            for (LifecycleObserver observer : observers) {
                LOG.debug("Notifying " + observer.getClass().getSimpleName() + " of shutdown");
                try {
                    observer.onExit(this);
                }
                catch (Throwable e) {
                    this.handle("Error notifying " + observer.getClass().getSimpleName() + " of shutdown", e);
                }
            }
            this.isClosed = true;
            this.isRunning = false;
        }
    }

    @Override
    public boolean isClosed() {
        return this.isClosed;
    }

    /**
     * @return the {@link ComponentProvider} that is used by this {@link ApplicationContext} to provide components
     */
    public PostProcessingComponentProvider componentProvider() {
        return this.componentProvider;
    }

    /**
     * Configuration for the {@link DelegatingApplicationContext}. This configuration is used to configure the
     * various components required by the {@link DelegatingApplicationContext} to function.
     *
     * @since 0.5.0
     *
     * @author Guus Lieben
     */
    public static class Configurer {

        private ContextualInitializer<ComponentRegistry, ? extends ComponentProviderOrchestrator> componentProvider = HierarchicalComponentProviderOrchestrator.create(Customizer.useDefaults());
        private ContextualInitializer<ApplicationContext, ? extends DefaultBindingConfigurer> defaultBindings = ContextualInitializer.of(DefaultBindingConfigurer::empty);

        /**
         * Configures the {@link PostProcessingComponentProvider} that is used by the {@link DelegatingApplicationContext} to
         * provide component instances.
         *
         * @param componentProvider the {@link PostProcessingComponentProvider} to use
         * @return the current instance
         */
        public Configurer componentProvider(ComponentProviderOrchestrator componentProvider) {
            return this.componentProvider(ContextualInitializer.of(componentProvider));
        }

        /**
         * Configures the {@link ComponentProvider} that is used by the {@link DelegatingApplicationContext} to provide
         * component instances.
         *
         * @param componentProvider the {@link PostProcessingComponentProvider} to use
         * @return the current instance
         */
        public Configurer componentProvider(ContextualInitializer<ComponentRegistry, ? extends ComponentProviderOrchestrator> componentProvider) {
            this.componentProvider = componentProvider;
            return this;
        }

        /**
         * Configures the {@link DefaultBindingConfigurer} that is used by the {@link DelegatingApplicationContext} to
         * configure bindings that should be available by default.
         *
         * @param defaultBindings the {@link DefaultBindingConfigurer} to use
         * @return the current instance
         */
        public Configurer defaultBindings(DefaultBindingConfigurer defaultBindings) {
            return this.defaultBindings(ContextualInitializer.of(defaultBindings));
        }

        /**
         * Configures the {@link DefaultBindingConfigurer} that is used by the {@link DelegatingApplicationContext} to
         * configure bindings that should be available by default.
         *
         * @param defaultBindings the {@link DefaultBindingConfigurer} to use
         * @return the current instance
         */
        public Configurer defaultBindings(BiConsumer<ApplicationContext, Binder> defaultBindings) {
            return this.defaultBindings(context -> binder -> defaultBindings.accept(context.input(), binder));
        }

        /**
         * Configures the {@link DefaultBindingConfigurer} that is used by the {@link DelegatingApplicationContext} to
         * configure bindings that should be available by default.
         *
         * @param defaultBindings the {@link DefaultBindingConfigurer} to use
         * @return the current instance
         */
        public Configurer defaultBindings(ContextualInitializer<ApplicationContext, ? extends DefaultBindingConfigurer> defaultBindings) {
            this.defaultBindings = defaultBindings;
            return this;
        }
    }
}
