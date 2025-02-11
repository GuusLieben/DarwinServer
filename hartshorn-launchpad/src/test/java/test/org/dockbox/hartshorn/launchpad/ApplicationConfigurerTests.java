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

package test.org.dockbox.hartshorn.launchpad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;
import java.util.stream.Collectors;

import org.dockbox.hartshorn.inject.LoggingExceptionHandler;
import org.dockbox.hartshorn.inject.provider.PostProcessingComponentProvider;
import org.dockbox.hartshorn.inject.InjectionCapableApplication;
import org.dockbox.hartshorn.inject.annotations.Component;
import org.dockbox.hartshorn.inject.component.ComponentRegistry;
import org.dockbox.hartshorn.inject.processing.ComponentPostProcessor;
import org.dockbox.hartshorn.inject.processing.ComponentPreProcessor;
import org.dockbox.hartshorn.inject.processing.ComponentProcessingContext;
import org.dockbox.hartshorn.inject.provider.ComponentProvider;
import org.dockbox.hartshorn.launchpad.ApplicationContext;
import org.dockbox.hartshorn.launchpad.DelegatingApplicationContext;
import org.dockbox.hartshorn.launchpad.HartshornApplication;
import org.dockbox.hartshorn.launchpad.HartshornApplicationConfigurer;
import org.dockbox.hartshorn.launchpad.ProcessableApplicationContext;
import org.dockbox.hartshorn.launchpad.activation.ServiceActivator;
import org.dockbox.hartshorn.launchpad.banner.HartshornLogoBanner;
import org.dockbox.hartshorn.launchpad.environment.ContextualApplicationEnvironment;
import org.dockbox.hartshorn.properties.PropertyRegistry;
import org.dockbox.hartshorn.util.Customizer;
import org.dockbox.hartshorn.util.TypeUtils;
import org.dockbox.hartshorn.util.collections.MultiMap;
import org.dockbox.hartshorn.util.introspect.scan.TypeReferenceCollectorContext;
import org.dockbox.hartshorn.util.introspect.scan.classpath.ClasspathTypeReferenceCollector;
import org.dockbox.hartshorn.util.option.Option;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class ApplicationConfigurerTests {

    private static ApplicationContext createApplication(Customizer<HartshornApplicationConfigurer> customizer) {
        return HartshornApplication.createApplication(ApplicationConfigurerTests.class).initialize(customizer.compose(config -> {
            config.includeBasePackages(false);
        }));
    }

    @Test
    @Disabled("#1121: Currently not integrated with property loaders") // TODO #1121: Fix this test
    @DisplayName("Customizer should be able to modify arguments provided to the application")
    void testArgumentsCustomizer() {
        ApplicationContext applicationContext = createApplication(configuration -> {
            configuration.arguments(arguments -> {
                arguments.add("--sample.x.y=z");
            });
        });
        PropertyRegistry propertyRegistry = applicationContext.environment().propertyRegistry();
        assertTrue(propertyRegistry.contains("sample.x.y"));
        String value = propertyRegistry.value("sample.x.y")
                .orElseThrow(() -> new AssertionError("Property not found"));
        assertEquals("z", value);
    }

    @Test
    @DisplayName("Customizer should be able to modify service activators")
    void testServiceActivatorsCustomizer() {
        ApplicationContext applicationContext = createApplication(configuration -> {
            configuration.activators(activators -> {
                activators.add(TypeUtils.annotation(UseSampleActivator.class));
            });
        });
        assertTrue(applicationContext.activators().hasActivator(UseSampleActivator.class));
    }

    @Test
    @DisplayName("Customizer should be able to modify default pre-processors")
    void testPreProcessorsCustomizer() {
        SamplePreProcessor processor = new SamplePreProcessor();
        ApplicationContext applicationContext = createApplication(configuration -> {
            configuration.componentPreProcessors(preProcessors -> {
                preProcessors.add(processor);
            });
        });
        ProcessableApplicationContext processableApplicationContext =
            assertInstanceOf(ProcessableApplicationContext.class, applicationContext);
        MultiMap<Integer, ComponentPreProcessor> processors = processableApplicationContext.defaultProvider()
                .processorRegistry().preProcessors();
        assertTrue(processors.containsValue(processor));
    }

    @Test
    @DisplayName("Customizer should be able to modify default post-processors")
    void testPostProcessorsCustomizer() {
        SamplePostProcessor processor = new SamplePostProcessor();
        ApplicationContext applicationContext = createApplication(configuration -> {
            configuration.componentPostProcessors(postProcessors -> {
                postProcessors.add(processor);
            });
        });
        DelegatingApplicationContext processableApplicationContext =
            assertInstanceOf(DelegatingApplicationContext.class, applicationContext);
        ComponentProvider componentProvider = processableApplicationContext.componentProvider();
        PostProcessingComponentProvider postProcessingComponentProvider =
            assertInstanceOf(PostProcessingComponentProvider.class, componentProvider);
        MultiMap<Integer, ComponentPostProcessor> processors = postProcessingComponentProvider.processorRegistry().postProcessors();
        assertTrue(processors.containsValue(processor));
    }

    @Test
    @DisplayName("Customizer should be able to modify standalone components")
    void testStandaloneComponentsCustomizer() {
        ApplicationContext applicationContext = createApplication(configuration -> {
            configuration.standaloneComponents(components -> {
                components.add(DummyUnmanagedComponent.class);
            });
        });
        ComponentRegistry componentRegistry = applicationContext.get(ComponentRegistry.class);
        assertTrue(componentRegistry.container(DummyUnmanagedComponent.class).present());
    }

    @Test
    @DisplayName("Customizer should be able to modify scanned packages")
    void testScannedPackagesCustomizer() {
        final String dummyPackage = "test.dummy.package";
        ApplicationContext applicationContext = createApplication(configuration -> {
            configuration.scanPackages(packages -> {
                packages.add(dummyPackage);
            });
        });
        Option<TypeReferenceCollectorContext> collectorContextCandidate =
            applicationContext.firstContext(TypeReferenceCollectorContext.class);
        assertTrue(collectorContextCandidate.present());

        TypeReferenceCollectorContext collectorContext = collectorContextCandidate.get();
        Set<String> packages = collectorContext.collectors().stream()
            .filter(ClasspathTypeReferenceCollector.class::isInstance)
            .map(ClasspathTypeReferenceCollector.class::cast)
            .map(ClasspathTypeReferenceCollector::packageName)
            .collect(Collectors.toSet());
        assertTrue(packages.contains(dummyPackage));
    }

    @Test
    @DisplayName("Customizer should be able to enable banner printing")
    void testBannerPrintingCustomizer() {
        Logger logger = LoggerFactory.getLogger(ApplicationConfigurerTests.class);
        OutputCaptureAppender appender = OutputCaptureAppender.registerForLogger(logger);
        createApplication(applicationConfigurer -> {
            // Banners are always disabled in CI environments. To avoid false negatives, simulate a non-CI environment
            applicationConfigurer.isBuildEnvironment(false);
            applicationConfigurer.enableBanner();
        });

        String expectedMessage = String.join("\n", new HartshornBannerAccessor().lines());
        for (ILoggingEvent event : appender.events()) {
            if (event.getMessage().equals(expectedMessage)) {
                return;
            }
        }
        fail("Expected message was not found in log output");
    }

    @Test
    @DisplayName("Customizer should be able to disable banner printing")
    void testBannerPrintingDisabledCustomizer() {
        Logger logger = LoggerFactory.getLogger(ApplicationConfigurerTests.class);
        OutputCaptureAppender appender = OutputCaptureAppender.registerForLogger(logger);
        createApplication(applicationConfigurer -> {
            // Banners are always disabled in CI environments. To avoid false positives, simulate a non-CI environment
            applicationConfigurer.isBuildEnvironment(false);
            applicationConfigurer.disableBanner();
        });

        String expectedMessage = String.join("\n", new HartshornBannerAccessor().lines());
        for (ILoggingEvent event : appender.events()) {
            if (event.getMessage().equals(expectedMessage)) {
                fail("Unexpected message found in log output");
            }
        }
    }

    @Test
    @DisplayName("Customizer should be able to enable batch mode")
    void testBatchModeCustomizer() {
        ApplicationContext applicationContext = createApplication(HartshornApplicationConfigurer::enableBatchMode);
        assertTrue(applicationContext.environment().isBatchMode());
    }

    @Test
    @DisplayName("Customizer should be able to disable batch mode")
    void testBatchModeDisabledCustomizer() {
        ApplicationContext applicationContext = createApplication(HartshornApplicationConfigurer::disableBatchMode);
        assertFalse(applicationContext.environment().isBatchMode());
    }

    @Test
    @DisplayName("Customizer should be able to enable strict mode")
    void testStrictModeCustomizer() {
        ApplicationContext applicationContext = createApplication(HartshornApplicationConfigurer::enableStrictMode);
        assertTrue(applicationContext.environment().isStrictMode());
    }

    @Test
    @DisplayName("Customizer should be able to disable strict mode")
    void testStrictModeDisabledCustomizer() {
        ApplicationContext applicationContext = createApplication(HartshornApplicationConfigurer::disableStrictMode);
        assertFalse(applicationContext.environment().isStrictMode());
    }

    @Test
    @DisplayName("Customizer should be able to enable stacktraces")
    void testShowStacktracesCustomizer() {
        ApplicationContext applicationContext = createApplication(HartshornApplicationConfigurer::showStacktraces);
        ContextualApplicationEnvironment contextualApplicationEnvironment =
            assertInstanceOf(ContextualApplicationEnvironment.class, applicationContext.environment());
        LoggingExceptionHandler exceptionHandler =
            assertInstanceOf(LoggingExceptionHandler.class, contextualApplicationEnvironment.exceptionHandler());
        assertTrue(exceptionHandler.printStackTraces());
    }

    @Test
    @DisplayName("Customizer should be able to disable stacktraces")
    void testHideStacktracesCustomizer() {
        ApplicationContext applicationContext = createApplication(HartshornApplicationConfigurer::hideStacktraces);
        ContextualApplicationEnvironment contextualApplicationEnvironment =
            assertInstanceOf(ContextualApplicationEnvironment.class, applicationContext.environment());
        LoggingExceptionHandler exceptionHandler =
            assertInstanceOf(LoggingExceptionHandler.class, contextualApplicationEnvironment.exceptionHandler());
        assertFalse(exceptionHandler.printStackTraces());
    }

    @Test
    @DisplayName("Customizer should be able to indicate build environment")
    void testBuildEnvironmentCustomizer() {
        ApplicationContext applicationContext = createApplication(configuration -> {
            configuration.isBuildEnvironment(true);
        });
        assertTrue(applicationContext.environment().isBuildEnvironment());
    }

    @Test
    @DisplayName("Customizer should be able to indicate non-build environment")
    void testNonBuildEnvironmentCustomizer() {
        ApplicationContext applicationContext = createApplication(configuration -> {
            configuration.isBuildEnvironment(false);
        });
        assertFalse(applicationContext.environment().isBuildEnvironment());
    }

    @Test
    @DisplayName("Customizer should be able to modify default bindings")
    void testDefaultBindingsCustomizer() {
        ApplicationContext applicationContext = createApplication(configuration -> {
            configuration.defaultBindings(bindings -> {
                bindings.bind(String.class).singleton("test");
            });
        });
        String value = applicationContext.get(String.class);
        assertEquals("test", value);
    }

    @ServiceActivator
    private @interface UseSampleActivator {
    }

    private static class SamplePreProcessor extends ComponentPreProcessor {

        @Override
        public <T> void process(InjectionCapableApplication application, ComponentProcessingContext<T> processingContext) {
            // Do nothing
        }

        @Override
        public int priority() {
            return 0;
        }
    }

    private static class SamplePostProcessor extends ComponentPostProcessor {
        @Override
        public int priority() {
            return 0;
        }
    }

    @Component
    private static class DummyUnmanagedComponent {
    }

    private static class HartshornBannerAccessor extends HartshornLogoBanner {
        @Override
        public Iterable<String> lines() {
            return super.lines();
        }
    }
}
