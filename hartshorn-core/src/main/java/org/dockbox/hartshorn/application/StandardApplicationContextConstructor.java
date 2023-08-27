/*
 * Copyright 2019-2023 the original author or authors.
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

package org.dockbox.hartshorn.application;

import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.application.context.ProcessableApplicationContext;
import org.dockbox.hartshorn.application.environment.ApplicationEnvironment;
import org.dockbox.hartshorn.application.environment.ContextualApplicationEnvironment;
import org.dockbox.hartshorn.application.lifecycle.LifecycleObserver;
import org.dockbox.hartshorn.application.lifecycle.ObservableApplicationEnvironment;
import org.dockbox.hartshorn.component.ComponentContainer;
import org.dockbox.hartshorn.component.ComponentLocator;
import org.dockbox.hartshorn.component.ComponentType;
import org.dockbox.hartshorn.component.UseProxying;
import org.dockbox.hartshorn.component.contextual.UseStaticBinding;
import org.dockbox.hartshorn.component.processing.ComponentPostProcessor;
import org.dockbox.hartshorn.component.processing.ComponentPreProcessor;
import org.dockbox.hartshorn.component.processing.ComponentProcessor;
import org.dockbox.hartshorn.component.processing.ServiceActivator;
import org.dockbox.hartshorn.inject.processing.UseContextInjection;
import org.dockbox.hartshorn.util.Customizer;
import org.dockbox.hartshorn.util.ContextualInitializer;
import org.dockbox.hartshorn.util.LazyStreamableConfigurer;
import org.dockbox.hartshorn.util.StreamableConfigurer;
import org.dockbox.hartshorn.util.TypeUtils;
import org.dockbox.hartshorn.util.introspect.scan.PredefinedSetTypeReferenceCollector;
import org.dockbox.hartshorn.util.introspect.scan.TypeReferenceCollectorContext;
import org.dockbox.hartshorn.util.introspect.scan.classpath.ClassPathScannerTypeReferenceCollector;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.option.Option;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class StandardApplicationContextConstructor implements ApplicationContextConstructor {

    private final ApplicationBuildContext buildContext;
    private final Configurer configurer;

    private StandardApplicationContextConstructor(ApplicationBuildContext buildContext, Configurer configurer) {
        this.buildContext = buildContext;
        this.configurer = configurer;
    }

    @Override
    public ApplicationContext createContext() {
        ApplicationBootstrapContext bootstrapContext = new ApplicationBootstrapContext(
                this.buildContext.mainClass(),
                this.buildContext.arguments(),
                this.configurer.includeBasePackages.initialize(this.buildContext)
        );

        ApplicationEnvironment environment = this.configurer.environment.initialize(bootstrapContext);
        ApplicationContext applicationContext = environment.applicationContext();

        this.configure(applicationContext, bootstrapContext);
        if (applicationContext instanceof ProcessableApplicationContext activatingApplicationContext) {
            activatingApplicationContext.loadContext();
        }
        this.finalize(applicationContext);

        return applicationContext;
    }

    private void registerHooks(ApplicationContext applicationContext) {
        this.buildContext.logger().debug("Registering shutdown hook for application context");
        ApplicationContextShutdownHook shutdownHook = new ApplicationContextShutdownHook(this.buildContext.logger(), applicationContext);
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook, "ShutdownHook"));
    }

    private void configure(ApplicationContext applicationContext, ApplicationBootstrapContext bootstrapContext) {
        ApplicationEnvironment environment = applicationContext.environment();

        TypeReferenceCollectorContext collectorContext = new TypeReferenceCollectorContext();

        Set<Annotation> activators = this.serviceActivators(applicationContext, bootstrapContext);
        this.enhanceTypeReferenceCollectorContext(bootstrapContext, environment, collectorContext, activators);

        ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContext(applicationContext, activators);
        applicationContext.add(serviceActivatorContext);

        Set<ServiceActivator> serviceActivatorAnnotations = activators.stream()
                .map(environment::introspect)
                .flatMap(introspected -> introspected.annotations().all(ServiceActivator.class).stream())
                .collect(Collectors.toSet());


        this.buildContext.logger().debug("Registering {} type reference collectors to application context", collectorContext.collectors().size());
        applicationContext.add(collectorContext);

        Set<Class<? extends ComponentProcessor>> processorTypes = serviceActivatorAnnotations.stream()
                .flatMap(serviceActivator -> Arrays.stream(serviceActivator.processors()))
                .collect(Collectors.toSet());
        // Create sets for ComponentPreProcessor and ComponentPostProcessor from processorTypes
        Set<Class<? extends ComponentPreProcessor>> preProcessorTypes = extractProcessors(processorTypes, ComponentPreProcessor.class);
        Set<Class<? extends ComponentPostProcessor>> postProcessorTypes = extractProcessors(processorTypes, ComponentPostProcessor.class);
        for (Class<? extends ComponentPostProcessor> postProcessorType : postProcessorTypes) {
            applicationContext.add(postProcessorType);
        }

        Set<ComponentProcessor> componentProcessors = this.componentProcessors(applicationContext, bootstrapContext, preProcessorTypes);

        this.buildContext.logger().debug("Registering {} component processors to application context", componentProcessors.size());
        for (ComponentProcessor componentProcessor : componentProcessors) {
            applicationContext.add(componentProcessor);
        }
    }

    private static <T extends ComponentProcessor> Set<Class<? extends T>> extractProcessors(Set<Class<? extends ComponentProcessor>> processorTypes, Class<T> processorClass) {
        return processorTypes.stream()
                .filter(processorClass::isAssignableFrom)
                .map(type -> (Class<? extends T>) type)
                .collect(Collectors.toSet());
    }

    private Set<Annotation> serviceActivators(ApplicationContext applicationContext, ApplicationBootstrapContext bootstrapContext) {
        Set<Annotation> activators = new HashSet<>(this.configurer.activators.initialize(bootstrapContext));
        Set<Annotation> serviceActivators = new HashSet<>(applicationContext.environment()
                .introspect(bootstrapContext.mainClass())
                .annotations()
                .annotedWith(ServiceActivator.class));
        activators.addAll(serviceActivators);

        for (Annotation activator : activators) {
            activators.addAll(this.serviceActivators(applicationContext.environment(), activator));
        }

        return activators;
    }

    private Set<Annotation> serviceActivators(ApplicationEnvironment environment, Annotation annotation) {
        TypeView<? extends Annotation> introspected = environment.introspect(annotation.annotationType());
        Set<Annotation> annotations = introspected.annotations().annotedWith(ServiceActivator.class);

        Set<Annotation> activators = new HashSet<>(annotations);
        for (Annotation activatorAnnotation : annotations) {
            activators.addAll(this.serviceActivators(environment, activatorAnnotation));
        }
        return activators;
    }

    @NotNull
    private Set<ComponentProcessor> componentProcessors(ApplicationContext applicationContext, ApplicationBootstrapContext bootstrapContext, Set<Class<? extends ComponentPreProcessor>> processorTypes) {
        Set<ComponentProcessor> componentProcessors = new HashSet<>();
        componentProcessors.addAll(this.configurer.componentPreProcessors.initialize(bootstrapContext));
        componentProcessors.addAll(this.configurer.componentPostProcessors.initialize(bootstrapContext));

        processorTypes.stream()
                .map(applicationContext::get)
                .forEach(componentProcessors::add);

        return componentProcessors;
    }

    private void enhanceTypeReferenceCollectorContext(ApplicationBootstrapContext bootstrapContext, ApplicationEnvironment environment, TypeReferenceCollectorContext collectorContext,
                                                      Set<Annotation> activators) {
        collectorContext.register(new ClassPathScannerTypeReferenceCollector(Hartshorn.PACKAGE_PREFIX));
        if (bootstrapContext.includeBasePackages()) {
            collectorContext.register(new ClassPathScannerTypeReferenceCollector(bootstrapContext.mainClass().getPackageName()));
        }

        Set<String> prefixes = new HashSet<>(this.configurer.scanPackages.initialize(bootstrapContext));
        for (ServiceActivator serviceActivator : environment.introspect(bootstrapContext.mainClass()).annotations().all(ServiceActivator.class)) {
            prefixes.addAll(List.of(serviceActivator.scanPackages()));
        }

        for (Annotation serviceActivator : activators) {
            Option<ServiceActivator> activatorCandidate = environment.introspect(serviceActivator).annotations().get(ServiceActivator.class);
            if (activatorCandidate.absent()) {
                throw new IllegalStateException("Service activator annotation " + serviceActivator + " is not annotated with @ServiceActivator");
            }

            ServiceActivator activator = activatorCandidate.get();
            prefixes.addAll(List.of(activator.scanPackages()));
        }

        prefixes.stream()
                .map(ClassPathScannerTypeReferenceCollector::new)
                .forEach(collectorContext::register);

        Set<Class<?>> standaloneComponents = Set.copyOf(this.configurer.standaloneComponents.initialize(bootstrapContext));
        if (!standaloneComponents.isEmpty()) {
            collectorContext.register(PredefinedSetTypeReferenceCollector.of(standaloneComponents));
        }
    }

    private void finalize(ApplicationContext applicationContext) {
        this.buildContext.logger().debug("Finalizing application context before releasing to application");
        if (applicationContext.environment() instanceof ObservableApplicationEnvironment observable) {
            this.buildContext.logger().debug("Notifying application environment observers of application context creation");
            for (LifecycleObserver observer : observable.observers(LifecycleObserver.class)) {
                observer.onStarted(applicationContext);
            }
        }

        for (ComponentContainer<?> container : applicationContext.get(ComponentLocator.class).containers(ComponentType.FUNCTIONAL)) {
            this.buildContext.logger().debug("Instantiating non-lazy singleton {} in application context", container.id());
            if (container.singleton() && !container.lazy()) {
                applicationContext.get(container.type().type());
            }
        }

        this.registerHooks(applicationContext);
    }

    public static ContextualInitializer<ApplicationBuildContext, StandardApplicationContextConstructor> create(Customizer<Configurer> customizer) {
        return buildContext -> {
            Configurer configurer = new Configurer();
            customizer.configure(configurer);
            return new StandardApplicationContextConstructor(buildContext, configurer);
        };
    }

    public static class Configurer extends ApplicationConfigurer {

        private final LazyStreamableConfigurer<ApplicationBootstrapContext, Annotation> activators = LazyStreamableConfigurer.of(
                TypeUtils.annotation(UseBootstrap.class),
                TypeUtils.annotation(UseProxying.class),
                TypeUtils.annotation(UseContextInjection.class),
                TypeUtils.annotation(UseStaticBinding.class)
        );

        private final LazyStreamableConfigurer<ApplicationBootstrapContext, ComponentPreProcessor> componentPreProcessors = LazyStreamableConfigurer.empty();
        private final LazyStreamableConfigurer<ApplicationBootstrapContext, ComponentPostProcessor> componentPostProcessors = LazyStreamableConfigurer.empty();
        private final LazyStreamableConfigurer<ApplicationBootstrapContext, Class<?>> standaloneComponents = LazyStreamableConfigurer.empty();
        private final LazyStreamableConfigurer<ApplicationBootstrapContext, String> scanPackages = LazyStreamableConfigurer.empty();

        private ContextualInitializer<ApplicationBootstrapContext, ? extends ApplicationEnvironment> environment = ContextualApplicationEnvironment.create(Customizer.useDefaults());
        private ContextualInitializer<ApplicationBuildContext, Boolean> includeBasePackages = ContextualInitializer.of(true);

        public Configurer activators(Customizer<StreamableConfigurer<ApplicationBootstrapContext, Annotation>> customizer) {
            this.activators.customizer(customizer);
            return this;
        }

        public Configurer componentPreProcessors(Customizer<StreamableConfigurer<ApplicationBootstrapContext, ComponentPreProcessor>> customizer) {
            this.componentPreProcessors.customizer(customizer);
            return this;
        }

        public Configurer componentPostProcessors(Customizer<StreamableConfigurer<ApplicationBootstrapContext, ComponentPostProcessor>> customizer) {
            this.componentPostProcessors.customizer(customizer);
            return this;
        }

        public Configurer standaloneComponents(Customizer<StreamableConfigurer<ApplicationBootstrapContext, Class<?>>> customizer) {
            this.standaloneComponents.customizer(customizer);
            return this;
        }

        public Configurer scanPackages(Customizer<StreamableConfigurer<ApplicationBootstrapContext, String>> customizer) {
            this.scanPackages.customizer(customizer);
            return this;
        }

        public Configurer environment(ApplicationEnvironment environment) {
            return this.environment(ContextualInitializer.of(environment));
        }

        public Configurer environment(ContextualInitializer<ApplicationBootstrapContext, ? extends ApplicationEnvironment> environment) {
            this.environment = environment;
            return this;
        }

        public Configurer includeBasePackages(boolean includeBasePackages) {
            return this.includeBasePackages(ContextualInitializer.of(includeBasePackages));
        }

        public Configurer includeBasePackages(ContextualInitializer<ApplicationBuildContext, Boolean> includeBasePackages) {
            this.includeBasePackages = includeBasePackages;
            return this;
        }
    }
}
