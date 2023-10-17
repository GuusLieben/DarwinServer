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

package org.dockbox.hartshorn.application.environment;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dockbox.hartshorn.application.ApplicationBootstrapContext;
import org.dockbox.hartshorn.application.ExceptionHandler;
import org.dockbox.hartshorn.application.LoggingExceptionHandler;
import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.application.context.SimpleApplicationContext;
import org.dockbox.hartshorn.application.environment.banner.Banner;
import org.dockbox.hartshorn.application.environment.banner.HartshornBanner;
import org.dockbox.hartshorn.application.environment.banner.ResourcePathBanner;
import org.dockbox.hartshorn.application.lifecycle.ObservableApplicationEnvironment;
import org.dockbox.hartshorn.application.lifecycle.Observer;
import org.dockbox.hartshorn.component.ComponentContainer;
import org.dockbox.hartshorn.component.ComponentLocator;
import org.dockbox.hartshorn.context.ModifiableContextCarrier;
import org.dockbox.hartshorn.discovery.DiscoveryService;
import org.dockbox.hartshorn.logging.ApplicationLogger;
import org.dockbox.hartshorn.logging.AutoSwitchingApplicationLogger;
import org.dockbox.hartshorn.logging.LogExclude;
import org.dockbox.hartshorn.proxy.ProxyOrchestrator;
import org.dockbox.hartshorn.util.ContextualInitializer;
import org.dockbox.hartshorn.util.Customizer;
import org.dockbox.hartshorn.util.SingleElementContext;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.IntrospectorLoader;
import org.dockbox.hartshorn.util.introspect.ProxyLookup;
import org.dockbox.hartshorn.util.introspect.SupplierAdapterProxyLookup;
import org.dockbox.hartshorn.util.introspect.annotations.AnnotationLookup;
import org.dockbox.hartshorn.util.introspect.annotations.VirtualHierarchyAnnotationLookup;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.option.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

@LogExclude
public final class ContextualApplicationEnvironment implements ObservableApplicationEnvironment, ModifiableContextCarrier {

    private final Set<Observer> observers = ConcurrentHashMap.newKeySet();
    private final Set<Class<? extends Observer>> lazyObservers = ConcurrentHashMap.newKeySet();
    private final EnvironmentTypeCollector typeCollector = new EnvironmentTypeCollector(this);

    private final FileSystemProvider fileSystemProvider;
    private final ApplicationLogger applicationLogger;
    private final ProxyOrchestrator proxyOrchestrator;
    private final ExceptionHandler exceptionHandler;
    private final AnnotationLookup annotationLookup;
    private final ClasspathResourceLocator resourceLocator;

    private final boolean isCI;
    private final boolean isBatchMode;

    private final ApplicationArgumentParser argumentParser;
    private final Properties arguments;

    private ApplicationContext applicationContext;
    private Introspector introspector;

    private ContextualApplicationEnvironment(SingleElementContext<? extends ApplicationBootstrapContext> context, Configurer configurer) {
        SingleElementContext<ApplicationEnvironment> environmentInitializerContext = context.transform(this);

        this.exceptionHandler = this.configure(environmentInitializerContext, configurer.exceptionHandler);
        this.annotationLookup = this.configure(environmentInitializerContext, configurer.annotationLookup);
        this.proxyOrchestrator = this.configure(environmentInitializerContext.transform(this.introspector()), configurer.proxyOrchestrator);
        this.applicationLogger = this.configure(environmentInitializerContext, configurer.applicationLogger);
        this.fileSystemProvider = this.configure(environmentInitializerContext, configurer.applicationFSProvider);
        this.argumentParser = this.configure(environmentInitializerContext, configurer.applicationArgumentParser);
        this.resourceLocator = this.configure(environmentInitializerContext, configurer.classpathResourceLocator);

        this.arguments = this.argumentParser.parse(context.input().arguments());

        SingleElementContext<Properties> argumentsInitializerContext = context.transform(this.arguments);
        this.printStacktraces(configurer.showStacktraces.initialize(argumentsInitializerContext));
        this.isBatchMode = configurer.enableBatchMode.initialize(argumentsInitializerContext);

        this.isCI = this.checkCI();
        this.checkForDebugging();

        if (!this.isCI && configurer.enableBanner.initialize(argumentsInitializerContext)) {
            this.printBanner(context.input().mainClass());
        }

        ApplicationContext initializedContext = configurer.applicationContext.initialize(environmentInitializerContext);
        // This will handle two aspects:
        // 1. If the context was not initialized through the implementation of ModifiableContextCarrier, it
        //    will be set here to the initialized context.
        // 2. If the context was initialized through the implementation of ModifiableContextCarrier, it will
        //    verify that the context is the same as the initialized context, or throw an exception to prevent
        //    the context from being overwritten and leaving the application in an inconsistent state.
        if (initializedContext != null) {
            this.applicationContext(initializedContext);
        }
    }

    private <I, T> T configure(SingleElementContext<I> context, ContextualInitializer<I, T> initializer) {
        T instance = initializer.initialize(context);
        return this.configure(instance);
    }

    @Override
    public Properties rawArguments() {
        return this.arguments;
    }

    private <T> T configure(T instance) {
        if (instance instanceof ApplicationManaged managed) {
            managed.environment(this);
        }
        return instance;
    }

    protected boolean checkCI() {
        return System.getenv().containsKey("GITLAB_CI")
                || System.getenv().containsKey("JENKINS_HOME")
                || System.getenv().containsKey("TRAVIS")
                || System.getenv().containsKey("GITHUB_ACTIONS")
                || System.getenv().containsKey("APPVEYOR");
    }

    public FileSystemProvider applicationFSProvider() {
        return this.fileSystemProvider;
    }

    public ApplicationLogger applicationLogger() {
        return this.applicationLogger;
    }

    public ExceptionHandler exceptionHandler() {
        return this.exceptionHandler;
    }

    public AnnotationLookup annotationLookup() {
        return this.annotationLookup;
    }

    @Override
    public FileSystemProvider fileSystem() {
        return this.fileSystemProvider;
    }

    @Override
    public ClasspathResourceLocator classpath() {
        return this.resourceLocator;
    }

    @Override
    public ProxyOrchestrator proxyOrchestrator() {
        return this.proxyOrchestrator;
    }

    @Override
    public Introspector introspector() {
        if (this.introspector == null) {
            // Lazy, as the proxy orchestrator may not yet be initialized
            ProxyLookup proxyLookup = new SupplierAdapterProxyLookup(() -> this.proxyOrchestrator);
            this.introspector = DiscoveryService.instance()
                    .discover(IntrospectorLoader.class)
                    .create(proxyLookup, this.annotationLookup());
        }
        return this.introspector;
    }

    @Override
    public boolean isCI() {
        return this.isCI;
    }

    @Override
    public boolean isBatchMode() {
        return this.isBatchMode;
    }

    @Override
    public <A extends Annotation> Collection<TypeView<?>> types(Class<A> annotation) {
        return this.typeCollector.types(type -> type.annotations().has(annotation));
    }

    @Override
    public <T> Collection<TypeView<? extends T>> children(Class<T> parent) {
        return this.typeCollector.types(type -> type.isChildOf(parent) && !type.is(parent));
    }

    @Override
    public List<Annotation> annotationsWith(TypeView<?> type, Class<? extends Annotation> annotation) {
        Collection<Annotation> annotations = new ArrayList<>();
        for (Annotation typeAnnotation : type.annotations().all()) {
            if (this.introspector().introspect(typeAnnotation.annotationType()).annotations().has(annotation)) {
                annotations.add(typeAnnotation);
            }
        }
        return List.copyOf(annotations);
    }

    @Override
    public List<Annotation> annotationsWith(Class<?> type, Class<? extends Annotation> annotation) {
        return this.annotationsWith(this.introspector().introspect(type), annotation);
    }

    @Override
    public boolean singleton(Class<?> type) {
        TypeView<?> typeView = this.introspector().introspect(type);
        return this.singleton(typeView);
    }

    @Override
    public boolean singleton(TypeView<?> type) {
        ComponentLocator componentLocator = this.applicationContext().get(ComponentLocator.class);
        return Boolean.TRUE.equals(componentLocator.container(type.type())
                .map(ComponentContainer::singleton)
                .orElseGet(() -> type.annotations().has(Singleton.class)));
    }

    @Override
    public ApplicationContext applicationContext() {
        return this.applicationContext;
    }

    @Override
    public void handle(Throwable throwable) {
        this.exceptionHandler.handle(throwable);
    }

    @Override
    public void handle(String message, Throwable throwable) {
        this.exceptionHandler.handle(message, throwable);
    }

    @Override
    public ExceptionHandler printStacktraces(boolean stacktraces) {
        return this.exceptionHandler.printStacktraces(stacktraces);
    }

    @Override
    public void register(Observer observer) {
        this.observers.add(observer);
    }

    @Override
    public void register(Class<? extends Observer> observer) {
        this.lazyObservers.add(observer);
    }

    @Override
    public Logger log() {
        return this.applicationLogger.log();
    }

    @Override
    public void enableDebugLogging(boolean active) {
        this.applicationLogger.enableDebugLogging(active);
    }

    private void checkForDebugging() {
        // TODO: Better property? This does not align with current property definition standard
        boolean debug = Boolean.TRUE.equals(Option.of(this.arguments.get("hartshorn:debug"))
                .cast(String.class)
                .map(Boolean::valueOf)
                .orElse(false));

        this.enableDebugLogging(debug);
    }

    @Override
    public <T extends Observer> Set<T> observers(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        Set<T> observers = new HashSet<>();
        this.observers.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .forEach(observers::add);

        this.lazyObservers.stream()
                .filter(type::isAssignableFrom)
                .map(this.applicationContext::get)
                .map(type::cast)
                .forEach(observers::add);

        return observers;
    }

    private void printBanner(Class<?> mainClass) {
        Logger logger = LoggerFactory.getLogger(mainClass);
        this.createBanner().print(logger);
    }

    private Banner createBanner() {
        return this.resourceLocator.resource("banner.txt")
                .option()
                .map(resource -> (Banner) new ResourcePathBanner(resource))
                .orElseGet(HartshornBanner::new);
    }

    /**
     * Creates a new {@link ContextualInitializer} for the {@link ContextualApplicationEnvironment} using the given
     * {@link Customizer}.
     *
     * @param customizer the customizer to use, if left empty the default configuration will be used
     * @return a non-cached {@link ContextualInitializer} for the {@link ContextualApplicationEnvironment}
     */
    public static ContextualInitializer<ApplicationBootstrapContext, ContextualApplicationEnvironment> create(Customizer<Configurer> customizer) {
        return context -> {
            Configurer configurer = new Configurer();
            customizer.configure(configurer);
            return new ContextualApplicationEnvironment(context, configurer);
        };
    }

    @Override
    public ModifiableContextCarrier applicationContext(ApplicationContext context) {
        if (this.applicationContext != null && this.applicationContext != context) {
            throw new IllegalStateException("Application context already set");
        }
        this.applicationContext = context;
        return this;
    }

    public static class Configurer {

        private ContextualInitializer<Properties, Boolean> enableBanner = ContextualInitializer.of(properties -> Boolean.valueOf(properties.getProperty("hartshorn.banner.enabled", "true")));
        private ContextualInitializer<Properties, Boolean> enableBatchMode = ContextualInitializer.of(properties -> Boolean.valueOf(properties.getProperty("hartshorn.batch.enabled", "false")));
        private ContextualInitializer<Properties, Boolean> showStacktraces = ContextualInitializer.of(properties -> Boolean.valueOf(properties.getProperty("hartshorn.exceptions.stacktraces", "true")));

        private ContextualInitializer<Introspector, ? extends ProxyOrchestrator> proxyOrchestrator = context -> DefaultProxyOrchestratorLoader.create(Customizer.useDefaults()).initialize(context);
        private ContextualInitializer<ApplicationEnvironment, ? extends FileSystemProvider> applicationFSProvider = ContextualInitializer.of(
                PathFileSystemProvider::new);
        private ContextualInitializer<ApplicationEnvironment, ? extends ExceptionHandler> exceptionHandler = ContextualInitializer.of(LoggingExceptionHandler::new);
        private ContextualInitializer<ApplicationEnvironment, ? extends ApplicationArgumentParser> applicationArgumentParser = ContextualInitializer.of(StandardApplicationArgumentParser::new);
        private ContextualInitializer<ApplicationEnvironment, ? extends ApplicationLogger> applicationLogger = AutoSwitchingApplicationLogger.create(Customizer.useDefaults());
        private ContextualInitializer<ApplicationEnvironment, ? extends ClasspathResourceLocator> classpathResourceLocator = ContextualInitializer.of(ClassLoaderClasspathResourceLocator::new);
        private ContextualInitializer<ApplicationEnvironment, ? extends AnnotationLookup> annotationLookup = ContextualInitializer.of(VirtualHierarchyAnnotationLookup::new);
        private ContextualInitializer<ApplicationEnvironment, ? extends ApplicationContext> applicationContext = SimpleApplicationContext.create(Customizer.useDefaults());

        /**
         * Enables or disables the banner. If the banner is enabled, it will be printed to the console when the
         * application starts. The banner is enabled by default.
         *
         * @param enableBanner whether to enable or disable the banner
         * @return the current {@link Configurer} instance
         */
        public Configurer enableBanner(ContextualInitializer<Properties, Boolean> enableBanner) {
            this.enableBanner = enableBanner;
            return this;
        }

        /**
         * Enables the banner. If the banner is enabled, it will be printed to the console when the application
         * starts. The banner is enabled by default.
         *
         * @return the current {@link Configurer} instance
         */
        public Configurer enableBanner() {
            return this.enableBanner(ContextualInitializer.of(true));
        }

        /**
         * Disables the banner. If the banner is disabled, it will not be printed to the console when the application
         * starts. The banner is enabled by default.
         *
         * @return the current {@link Configurer} instance
         */
        public Configurer disableBanner() {
            return this.enableBanner(ContextualInitializer.of(false));
        }

        /**
         * Enables or disables batch mode. Batch mode is typically used for optimizations specific to applications
         * which will spawn multiple application contexts with shared resources. Batch mode is disabled by default.
         *
         * @param enableBatchMode whether to enable or disable batch mode
         * @return the current {@link Configurer} instance
         */
        public Configurer enableBatchMode(ContextualInitializer<Properties, Boolean> enableBatchMode) {
            this.enableBatchMode = enableBatchMode;
            return this;
        }

        /**
         * Enables batch mode. Batch mode is typically used for optimizations specific to applications which will
         * spawn multiple application contexts with shared resources. Batch mode is disabled by default.
         *
         * @return the current {@link Configurer} instance
         */
        public Configurer enableBatchMode() {
            return this.enableBatchMode(ContextualInitializer.of(true));
        }

        /**
         * Disables batch mode. Batch mode is typically used for optimizations specific to applications which will
         * spawn multiple application contexts with shared resources. Batch mode is disabled by default.
         *
         * @return the current {@link Configurer} instance
         */
        public Configurer disableBatchMode() {
            return this.enableBatchMode(ContextualInitializer.of(false));
        }

        /**
         * Enables or disables the printing of stacktraces when exceptions occur. Stacktraces are enabled by default.
         *
         * @param showStacktraces whether to enable or disable stacktraces
         * @return the current {@link Configurer} instance
         */
        public Configurer showStacktraces(ContextualInitializer<Properties, Boolean> showStacktraces) {
            this.showStacktraces = showStacktraces;
            return this;
        }

        /**
         * Enables the printing of stacktraces when exceptions occur. Stacktraces are enabled by default.
         *
         * @return the current {@link Configurer} instance
         */
        public Configurer showStacktraces() {
            return this.showStacktraces(ContextualInitializer.of(true));
        }

        /**
         * Disables the printing of stacktraces when exceptions occur. Stacktraces are enabled by default.
         *
         * @return the current {@link Configurer} instance
         */
        public Configurer hideStacktraces() {
            return this.showStacktraces(ContextualInitializer.of(false));
        }

        /**
         * Sets the {@link ProxyOrchestrator} to use. The {@link ProxyOrchestrator} is responsible for creating
         * proxies for application components. The default implementation is provided by {@link DefaultProxyOrchestratorLoader}.
         *
         * @param proxyOrchestrator the {@link ProxyOrchestrator} to use
         * @see ProxyOrchestrator
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationOrchestrator(ProxyOrchestrator proxyOrchestrator) {
            return this.applicationOrchestrator(ContextualInitializer.of(proxyOrchestrator));
        }

        /**
         * Sets the {@link ProxyOrchestrator} to use. The {@link ProxyOrchestrator} is responsible for creating
         * proxies for application components. The default implementation is provided by {@link DefaultProxyOrchestratorLoader}.
         *
         * @param orchestrator the {@link ProxyOrchestrator} to use
         * @see ProxyOrchestrator
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationOrchestrator(ContextualInitializer<Introspector, ? extends ProxyOrchestrator> orchestrator) {
            this.proxyOrchestrator = orchestrator;
            return this;
        }

        /**
         * Sets the {@link FileSystemProvider} to use. The {@link FileSystemProvider} is responsible for
         * providing the application's file system. The default implementation is {@link PathFileSystemProvider}.
         *
         * @param fileSystemProvider the {@link FileSystemProvider} to use
         * @see FileSystemProvider
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationFSProvider(FileSystemProvider fileSystemProvider) {
            return this.applicationFSProvider(ContextualInitializer.of(fileSystemProvider));
        }

        /**
         * Sets the {@link FileSystemProvider} to use. The {@link FileSystemProvider} is responsible for
         * providing the application's file system. The default implementation is {@link PathFileSystemProvider}.
         *
         * @param applicationFSProvider the {@link FileSystemProvider} to use
         * @see FileSystemProvider
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationFSProvider(ContextualInitializer<ApplicationEnvironment, ? extends FileSystemProvider> applicationFSProvider) {
            this.applicationFSProvider = applicationFSProvider;
            return this;
        }

        /**
         * Sets the {@link ExceptionHandler} to use. The {@link ExceptionHandler} is responsible for handling
         * exceptions that occur during the application's lifecycle. The default implementation is {@link LoggingExceptionHandler}.
         *
         * @param exceptionHandler the {@link ExceptionHandler} to use
         * @see ExceptionHandler
         * @return the current {@link Configurer} instance
         */
        public Configurer exceptionHandler(ExceptionHandler exceptionHandler) {
            return this.exceptionHandler(ContextualInitializer.of(exceptionHandler));
        }

        /**
         * Sets the {@link ExceptionHandler} to use. The {@link ExceptionHandler} is responsible for handling
         * exceptions that occur during the application's lifecycle. The default implementation is {@link LoggingExceptionHandler}.
         *
         * @param exceptionHandler the {@link ExceptionHandler} to use
         * @see ExceptionHandler
         * @return the current {@link Configurer} instance
         */
        public Configurer exceptionHandler(ContextualInitializer<ApplicationEnvironment, ? extends ExceptionHandler> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * Sets the {@link ApplicationArgumentParser} to use. The {@link ApplicationArgumentParser} is responsible for
         * parsing arguments passed to the application. The default implementation is {@link StandardApplicationArgumentParser}.
         *
         * @param applicationArgumentParser the {@link ApplicationArgumentParser} to use
         * @see ApplicationArgumentParser
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationArgumentParser(ApplicationArgumentParser applicationArgumentParser) {
            return this.applicationArgumentParser(ContextualInitializer.of(applicationArgumentParser));
        }

        /**
         * Sets the {@link ApplicationArgumentParser} to use. The {@link ApplicationArgumentParser} is responsible for
         * parsing arguments passed to the application. The default implementation is {@link StandardApplicationArgumentParser}.
         *
         * @param applicationArgumentParser the {@link ApplicationArgumentParser} to use
         * @see ApplicationArgumentParser
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationArgumentParser(ContextualInitializer<ApplicationEnvironment, ? extends ApplicationArgumentParser> applicationArgumentParser) {
            this.applicationArgumentParser = applicationArgumentParser;
            return this;
        }

        /**
         * Sets the {@link ApplicationLogger} to use. The {@link ApplicationLogger} is responsible for logging
         * messages during the application's lifecycle. The default implementation is {@link AutoSwitchingApplicationLogger}.
         *
         * @param applicationLogger the {@link ApplicationLogger} to use
         * @see ApplicationLogger
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationLogger(ApplicationLogger applicationLogger) {
            return this.applicationLogger(ContextualInitializer.of(applicationLogger));
        }

        /**
         * Sets the {@link ApplicationLogger} to use. The {@link ApplicationLogger} is responsible for logging
         * messages during the application's lifecycle. The default implementation is {@link AutoSwitchingApplicationLogger}.
         *
         * @param applicationLogger the {@link ApplicationLogger} to use
         * @see ApplicationLogger
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationLogger(ContextualInitializer<ApplicationEnvironment, ? extends ApplicationLogger> applicationLogger) {
            this.applicationLogger = applicationLogger;
            return this;
        }

        /**
         * Sets the {@link ClasspathResourceLocator} to use. The {@link ClasspathResourceLocator} is responsible for
         * locating resources on the classpath. The default implementation is {@link ClassLoaderClasspathResourceLocator}.
         *
         * @param classpathResourceLocator the {@link ClasspathResourceLocator} to use
         * @see ClasspathResourceLocator
         * @return the current {@link Configurer} instance
         */
        public Configurer classpathResourceLocator(ClasspathResourceLocator classpathResourceLocator) {
            return this.classpathResourceLocator(ContextualInitializer.of(classpathResourceLocator));
        }

        /**
         * Sets the {@link ClasspathResourceLocator} to use. The {@link ClasspathResourceLocator} is responsible for
         * locating resources on the classpath. The default implementation is {@link ClassLoaderClasspathResourceLocator}.
         *
         * @param classpathResourceLocator the {@link ClasspathResourceLocator} to use
         * @see ClasspathResourceLocator
         * @return the current {@link Configurer} instance
         */
        public Configurer classpathResourceLocator(ContextualInitializer<ApplicationEnvironment, ? extends ClasspathResourceLocator> classpathResourceLocator) {
            this.classpathResourceLocator = classpathResourceLocator;
            return this;
        }

        /**
         * Sets the {@link AnnotationLookup} to use. The {@link AnnotationLookup} is responsible for looking up
         * annotations on elements. The default implementation is {@link VirtualHierarchyAnnotationLookup}.
         *
         * @param annotationLookup the {@link AnnotationLookup} to use
         * @see AnnotationLookup
         * @return the current {@link Configurer} instance
         */
        public Configurer annotationLookup(AnnotationLookup annotationLookup) {
            return this.annotationLookup(ContextualInitializer.of(annotationLookup));
        }

        /**
         * Sets the {@link AnnotationLookup} to use. The {@link AnnotationLookup} is responsible for looking up
         * annotations on elements. The default implementation is {@link VirtualHierarchyAnnotationLookup}.
         *
         * @param annotationLookup the {@link AnnotationLookup} to use
         * @see AnnotationLookup
         * @return the current {@link Configurer} instance
         */
        public Configurer annotationLookup(ContextualInitializer<ApplicationEnvironment, ? extends AnnotationLookup> annotationLookup) {
            this.annotationLookup = annotationLookup;
            return this;
        }

        /**
         * Sets the {@link ApplicationContext} to use. The {@link ApplicationContext} is responsible for providing
         * access to components and global application state. The default implementation is {@link SimpleApplicationContext}.
         *
         * @param applicationContext the {@link ApplicationContext} to use
         * @see ApplicationContext
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationContext(ApplicationContext applicationContext) {
            return this.applicationContext(ContextualInitializer.of(applicationContext));
        }

        /**
         * Sets the {@link ApplicationContext} to use. The {@link ApplicationContext} is responsible for providing
         * access to components and global application state. The default implementation is {@link SimpleApplicationContext}.
         *
         * @param applicationContext the {@link ApplicationContext} to use
         * @see ApplicationContext
         * @return the current {@link Configurer} instance
         */
        public Configurer applicationContext(ContextualInitializer<ApplicationEnvironment, ? extends ApplicationContext> applicationContext) {
            this.applicationContext = applicationContext;
            return this;
        }
    }
}
