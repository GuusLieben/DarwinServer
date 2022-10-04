/*
 * Copyright 2019-2022 the original author or authors.
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

import org.dockbox.hartshorn.application.ExceptionHandler;
import org.dockbox.hartshorn.application.InitializingContext;
import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.application.context.IllegalModificationException;
import org.dockbox.hartshorn.application.environment.banner.Banner;
import org.dockbox.hartshorn.application.environment.banner.HartshornBanner;
import org.dockbox.hartshorn.application.environment.banner.ResourcePathBanner;
import org.dockbox.hartshorn.application.lifecycle.ObservableApplicationEnvironment;
import org.dockbox.hartshorn.application.lifecycle.Observer;
import org.dockbox.hartshorn.application.scan.PrefixContext;
import org.dockbox.hartshorn.component.ComponentContainer;
import org.dockbox.hartshorn.component.ComponentLocator;
import org.dockbox.hartshorn.context.ModifiableContextCarrier;
import org.dockbox.hartshorn.logging.ApplicationLogger;
import org.dockbox.hartshorn.logging.LogExclude;
import org.dockbox.hartshorn.proxy.ApplicationProxier;
import org.dockbox.hartshorn.proxy.ProxyManager;
import org.dockbox.hartshorn.proxy.StateAwareProxyFactory;
import org.dockbox.hartshorn.util.Result;
import org.dockbox.hartshorn.util.introspect.ElementAnnotationsIntrospector;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.annotations.AnnotationLookup;
import org.dockbox.hartshorn.util.introspect.annotations.DuplicateAnnotationCompositeException;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionIntrospector;
import org.dockbox.hartshorn.util.introspect.view.ConstructorView;
import org.dockbox.hartshorn.util.introspect.view.FieldView;
import org.dockbox.hartshorn.util.introspect.view.MethodView;
import org.dockbox.hartshorn.util.introspect.view.ParameterView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

@LogExclude
public class ContextualApplicationEnvironment implements ObservableApplicationEnvironment, ModifiableContextCarrier {

    private final Set<Observer> observers = ConcurrentHashMap.newKeySet();
    private final Set<Class<? extends Observer>> lazyObservers = ConcurrentHashMap.newKeySet();
    private final ApplicationFSProvider applicationFSProvider;
    private final ApplicationLogger applicationLogger;
    private final ApplicationProxier applicationProxier;
    private final ExceptionHandler exceptionHandler;
    private final AnnotationLookup annotationLookup;
    private final boolean isCI;

    private ApplicationContext applicationContext;
    private final PrefixContext prefixContext;
    private Introspector introspector;

    public ContextualApplicationEnvironment(final InitializingContext initializingContext) {
        final InitializingContext context = new InitializingContext(this, null, initializingContext.builder());

        this.exceptionHandler = this.configure(context.exceptionHandler());
        this.applicationFSProvider = this.configure(context.applicationFSProvider());
        this.applicationLogger = this.configure(context.applicationLogger());
        this.applicationProxier = this.configure(context.applicationProxier());
        this.annotationLookup = this.configure(context.annotationLookup());

        this.isCI = this.checkCI();
        this.checkForDebugging(context);

        if (!this.isCI() && context.builder().enableBanner())
            this.printBanner(context);

        this.prefixContext = context.prefixContext();
        this.log().debug("Created new application environment (isCI: %s, prefixCount: %d)".formatted(this.isCI(), this.prefixContext().prefixes().size()));
    }

    private <T> T configure(final T instance) {
        if (instance instanceof ApplicationManaged managed)
            managed.environment(this);
        return instance;
    }

    protected boolean checkCI() {
        return System.getenv().containsKey("GITLAB_CI")
                || System.getenv().containsKey("JENKINS_HOME")
                || System.getenv().containsKey("TRAVIS")
                || System.getenv().containsKey("GITHUB_ACTIONS")
                || System.getenv().containsKey("APPVEYOR");
    }

    public ApplicationFSProvider applicationFSProvider() {
        return applicationFSProvider;
    }

    public ApplicationLogger applicationLogger() {
        return applicationLogger;
    }

    public ApplicationProxier applicationProxier() {
        return applicationProxier;
    }

    public ExceptionHandler exceptionHandler() {
        return exceptionHandler;
    }

    public AnnotationLookup annotationLookup() {
        return annotationLookup;
    }

    @Override
    public PrefixContext prefixContext() {
        return this.prefixContext;
    }

    @Override
    public Introspector introspector() {
        if (this.introspector == null) {
            this.introspector = new ReflectionIntrospector(this.applicationContext());
        }
        return this.introspector;
    }

    @Override
    public boolean isCI() {
        return this.isCI;
    }

    @Override
    public void prefix(final String prefix) {
        this.prefixContext.prefix(prefix);
    }

    @Override
    public <A extends Annotation> Collection<TypeView<?>> types(final Class<A> annotation) {
        return this.prefixContext.types(annotation);
    }

    @Override
    public <A extends Annotation> Collection<TypeView<?>> types(final String prefix, final Class<A> annotation, final boolean skipParents) {
        return this.prefixContext.types(prefix, annotation, skipParents);
    }

    @Override
    public <A extends Annotation> Collection<TypeView<?>> types(final Class<A> annotation, final boolean skipParents) {
        return this.prefixContext.types(annotation, skipParents);
    }

    @Override
    public <T> Collection<TypeView<? extends T>> children(final TypeView<T> parent) {
        return this.prefixContext.children(parent);
    }

    @Override
    public <T> Collection<TypeView<? extends T>> children(final Class<T> parent) {
        return this.prefixContext.children(parent);
    }

    @Override
    public List<Annotation> annotationsWith(final TypeView<?> type, final Class<? extends Annotation> annotation) {
        final Collection<Annotation> annotations = new ArrayList<>();
        for (final Annotation typeAnnotation : type.annotations().all()) {
            if (this.introspect(typeAnnotation.annotationType()).annotations().has(annotation)) {
                annotations.add(typeAnnotation);
            }
        }
        return List.copyOf(annotations);
    }

    @Override
    public List<Annotation> annotationsWith(final Class<?> type, final Class<? extends Annotation> annotation) {
        return this.annotationsWith(this.introspect(type), annotation);
    }

    @Override
    public boolean singleton(final Class<?> type) {
        final TypeView<?> typeView = this.introspect(type);
        return this.singleton(typeView);
    }

    @Override
    public boolean singleton(final TypeView<?> type) {
        final ComponentLocator componentLocator = this.applicationContext().get(ComponentLocator.class);
        return Boolean.TRUE.equals(componentLocator.container(type.type())
                .map(ComponentContainer::singleton)
                .orElse(() -> type.annotations().has(Singleton.class))
                .or(false));
    }

    @Override
    public <T> TypeView<T> introspect(final Class<T> type) {
        return this.introspector().introspect(type);
    }

    @Override
    public <T> TypeView<T> introspect(final T instance) {
        return this.introspector().introspect(instance);
    }

    @Override
    public TypeView<?> introspect(final Type type) {
        return this.introspector().introspect(type);
    }

    @Override
    public TypeView<?> introspect(final ParameterizedType type) {
        return this.introspector().introspect(type);
    }

    @Override
    public TypeView<?> introspect(final String type) {
        return this.introspector().introspect(type);
    }

    @Override
    public MethodView<?, ?> introspect(final Method method) {
        return this.introspector().introspect(method);
    }

    @Override
    public <T> ConstructorView<T> introspect(final Constructor<T> method) {
        return this.introspector().introspect(method);
    }

    @Override
    public FieldView<?, ?> introspect(final Field field) {
        return this.introspector().introspect(field);
    }

    @Override
    public ParameterView<?> introspect(final Parameter parameter) {
        return this.introspector().introspect(parameter);
    }

    @Override
    public ElementAnnotationsIntrospector introspect(final AnnotatedElement annotatedElement) {
        return this.introspector().introspect(annotatedElement);
    }

    @Override
    public ApplicationContext applicationContext() {
        return this.applicationContext;
    }

    @Override
    public void handle(final Throwable throwable) {
        this.exceptionHandler.handle(throwable);
    }

    @Override
    public void handle(final String message, final Throwable throwable) {
        this.exceptionHandler.handle(message, throwable);
    }

    @Override
    public ExceptionHandler stacktraces(final boolean stacktraces) {
        return this.exceptionHandler.stacktraces(stacktraces);
    }

    @Override
    public Path applicationPath() {
        return this.applicationFSProvider.applicationPath();
    }

    @Override
    public void register(final Observer observer) {
        this.observers.add(observer);
    }

    @Override
    public void register(final Class<? extends Observer> observer) {
        this.lazyObservers.add(observer);
    }

    @Override
    public Logger log() {
        return this.applicationLogger.log();
    }

    @Override
    public void setDebugActive(final boolean active) {
        this.applicationLogger.setDebugActive(active);
    }

    private void checkForDebugging(final InitializingContext context) {
        final Set<String> arguments = context.builder().arguments();
        final ApplicationArgumentParser parser = context.argumentParser();

        final boolean debug = Boolean.TRUE.equals(Result.of(parser.parse(arguments).get("hartshorn:debug"))
                .map(String.class::cast)
                .map(Boolean::valueOf)
                .or(false));

        this.setDebugActive(debug);
    }

    @Override
    public <T> Result<Class<T>> real(final T instance) {
        return this.applicationProxier.real(instance);
    }

    @Override
    public <T> Result<ProxyManager<T>> manager(final T instance) {
        return this.applicationProxier.manager(instance);
    }

    @Override
    public <D, T extends D> Result<D> delegate(final TypeView<D> type, final T instance) {
        return this.applicationProxier.delegate(type, instance);
    }

    @Override
    public <T> StateAwareProxyFactory<T, ?> factory(final TypeView<T> type) {
        return this.applicationProxier.factory(type);
    }

    @Override
    public <T> StateAwareProxyFactory<T, ?> factory(final Class<T> type) {
        return this.applicationProxier.factory(type);
    }

    @Override
    public <T> Class<T> unproxy(final T instance) {
        return this.applicationProxier.unproxy(instance);
    }

    @Override
    public boolean isProxy(final Object instance) {
        return this.applicationProxier.isProxy(instance);
    }

    @Override
    public boolean isProxy(final Class<?> candidate) {
        return this.applicationProxier.isProxy(candidate);
    }

    @Override
    public <T extends Observer> Set<T> observers(final Class<T> type) {
        if (type == null) throw new IllegalArgumentException("type cannot be null");

        final Set<T> observers = new HashSet<>();
        this.observers.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .forEach(observers::add);

        this.lazyObservers.stream()
                .filter(type::isAssignableFrom)
                .map(lo -> this.applicationContext.get(lo))
                .map(type::cast)
                .forEach(observers::add);

        return observers;
    }

    @Override
    public ModifiableContextCarrier applicationContext(final ApplicationContext applicationContext) {
        if (this.applicationContext == null) this.applicationContext = applicationContext;
        else throw new IllegalModificationException("Application context has already been configured");
        return this;
    }

    private void printBanner(final InitializingContext context) {
        final Logger logger = LoggerFactory.getLogger(context.builder().mainClass());
        this.createBanner(context).print(logger);
    }

    private Banner createBanner(final InitializingContext context) {
        final ClasspathResourceLocator resourceLocator = context.resourceLocator();
        return resourceLocator.resource("banner.txt")
                .discardError() // Ignore missing banner exception
                .map(resource -> (Banner) new ResourcePathBanner(resource))
                .orElse(HartshornBanner::new)
                .get();
    }

    @Override
    public <A extends Annotation> A find(final AnnotatedElement element, final Class<A> annotationType) throws DuplicateAnnotationCompositeException {
        return this.annotationLookup.find(element, annotationType);
    }

    @Override
    public <A extends Annotation> List<A> findAll(final AnnotatedElement element, final Class<A> annotationType) {
        return this.annotationLookup.findAll(element, annotationType);
    }

    @Override
    public Annotation unproxy(final Annotation annotation) {
        return this.annotationLookup.unproxy(annotation);
    }

    @Override
    public LinkedHashSet<Class<? extends Annotation>> annotationHierarchy(final Class<? extends Annotation> klass) {
        return this.annotationLookup.annotationHierarchy(klass);
    }
}
