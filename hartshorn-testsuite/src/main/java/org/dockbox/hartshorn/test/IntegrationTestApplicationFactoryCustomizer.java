/*
 * Copyright 2019-2025 the original author or authors.
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

package org.dockbox.hartshorn.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dockbox.hartshorn.inject.ObjectFactory;
import org.dockbox.hartshorn.inject.ReflectionObjectFactory;
import org.dockbox.hartshorn.inject.processing.ComponentPostProcessor;
import org.dockbox.hartshorn.inject.processing.ComponentPreProcessor;
import org.dockbox.hartshorn.launchpad.SimpleApplicationContext;
import org.dockbox.hartshorn.launchpad.activation.ServiceActivator;
import org.dockbox.hartshorn.launchpad.environment.ContextualApplicationEnvironment;
import org.dockbox.hartshorn.launchpad.launch.StandardApplicationContextFactory;
import org.dockbox.hartshorn.test.annotations.TestBinding;
import org.dockbox.hartshorn.test.annotations.TestComponents;
import org.dockbox.hartshorn.test.junit.HartshornIntegrationTest;
import org.dockbox.hartshorn.util.Customizer;
import org.dockbox.hartshorn.util.option.Option;

/**
 * Customizer for the {@link StandardApplicationContextFactory} that configures the application context for integration tests.
 *
 * @param testClass the test class
 * @param testComponentSources the component sources to use for the test
 */
public record IntegrationTestApplicationFactoryCustomizer(
        Class<?> testClass,
        List<AnnotatedElement> testComponentSources
) implements Customizer<StandardApplicationContextFactory.Configurer> {

    private static final ObjectFactory OBJECT_FACTORY = new ReflectionObjectFactory();

    @Override
    public void configure(StandardApplicationContextFactory.Configurer constructor) {
        Customizer<ContextualApplicationEnvironment.Configurer> environmentCustomizer = environment -> {
            environment.disableBanner(); // Disable banner for tests, to avoid unnecessary noise
            environment.enableBatchMode(); // Enable batch mode, to make use of additional caching between tests. This decreases startup time after warmup (first test).
            environment.showStacktraces(); // Enable stacktraces for tests, to make debugging easier
            environment.applicationFSProvider(new TemporaryFileSystemProvider());

            Customizer<SimpleApplicationContext.Configurer> applicationContextCustomizer = applicationContext -> {
                this.configureDefaultBindings(applicationContext, this.testComponentSources);
            };
            environment.applicationContext(
                    SimpleApplicationContext.create(applicationContextCustomizer.compose(TestCustomizer.APPLICATION_CONTEXT.customizer())));
        };
        constructor.environment(
                ContextualApplicationEnvironment.create(environmentCustomizer.compose(TestCustomizer.ENVIRONMENT.customizer())));

        for(AnnotatedElement element : this.testComponentSources) {
            this.customizeWithComponentSource(constructor, element);
        }

        this.customizeActivators(constructor);
    }

    private void configureDefaultBindings(SimpleApplicationContext.Configurer applicationContext,
            List<AnnotatedElement> testComponentSources) {
        for(AnnotatedElement testComponentSource : testComponentSources) {
            if(testComponentSource.isAnnotationPresent(TestComponents.class)) {
                TestBinding[] bindings = testComponentSource.getAnnotation(TestComponents.class).bindings();
                applicationContext.defaultBindings((context, binder) -> {
                    for(TestBinding binding : bindings) {
                        //noinspection unchecked
                        binder.bind(binding.type()).to((Class) binding.implementation());
                    }
                });
            }
        }
    }

    private void customizeActivators(StandardApplicationContextFactory.Configurer constructor) {
        Class<?> next = this.testClass;
        Set<Annotation> serviceActivators = new HashSet<>();
        while(next != null) {
            Arrays.stream(next.getAnnotations())
                    .filter(annotation -> annotation.annotationType().isAnnotationPresent(ServiceActivator.class))
                    .forEach(serviceActivators::add);

            next = next.getSuperclass();
        }
        constructor.activators(activators -> {
            activators.addAll(serviceActivators);
        });
    }

    private void customizeWithComponentSource(StandardApplicationContextFactory.Configurer constructor, AnnotatedElement element) {
        Option<HartshornIntegrationTest> testDecorator = Option.of(element.getAnnotation(HartshornIntegrationTest.class));
        if(testDecorator.present()) {
            this.registerProcessors(constructor, testDecorator.get());
            constructor.scanPackages(config -> config.addAll(testDecorator.get().scanPackages()));
            constructor.includeBasePackages(testDecorator.get().includeBasePackages());
        }
        registerStandaloneComponents(constructor, element);
    }

    private void registerProcessors(StandardApplicationContextFactory.Configurer constructor, HartshornIntegrationTest testDecorator) {
        // Deprecated approach, retained for backwards compatibility
        @Deprecated(since = "0.7.0", forRemoval = true)
        List<Class<?>> processors = List.of(testDecorator.processors());
        List<ComponentPreProcessor> preProcessors = this.filterAndInstantiate(ComponentPreProcessor.class, processors);
        constructor.componentPreProcessors(config -> config.addAll(preProcessors));
        List<ComponentPostProcessor> postProcessors = this.filterAndInstantiate(ComponentPostProcessor.class, processors);
        constructor.componentPostProcessors(config -> config.addAll(postProcessors));

        // New approach (dedicated attributes for each processor type)
        constructor.componentPreProcessors(config -> config.addAll(this.instantiateAll(List.of(testDecorator.componentPreProcessors()))));
        constructor.componentPostProcessors(config -> config.addAll(this.instantiateAll(List.of(testDecorator.componentPostProcessors()))));
        constructor.binderPostProcessors(config -> config.addAll(this.instantiateAll(List.of(testDecorator.binderPostProcessors()))));
    }

    private static void registerStandaloneComponents(StandardApplicationContextFactory.Configurer constructor, AnnotatedElement element) {
        if(element.isAnnotationPresent(TestComponents.class)) {
            TestComponents testComponents = element.getAnnotation(TestComponents.class);
            constructor.standaloneComponents(components -> components.addAll(testComponents.components()));
        }
    }

    @Deprecated(since = "0.7.0", forRemoval = true)
    private <T> List<T> filterAndInstantiate(Class<T> type, List<Class<?>> processors) {
        List<T> result = new ArrayList<>();
        for(Class<?> processor : processors) {
            if(type.isAssignableFrom(processor)) {
                Object instance = OBJECT_FACTORY.create(processor);
                result.add(type.cast(instance));
            }
        }
        return result;
    }

    private <T> List<T> instantiateAll(List<Class<? extends T>> types) {
        List<T> result = new ArrayList<>();
        for(Class<? extends T> type : types) {
            result.add(OBJECT_FACTORY.create(type));
        }
        return result;
    }
}
