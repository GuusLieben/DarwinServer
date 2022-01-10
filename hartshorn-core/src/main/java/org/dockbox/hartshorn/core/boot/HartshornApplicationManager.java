/*
 * Copyright (C) 2020 Guus Lieben
 *
 * This framework is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see {@literal<http://www.gnu.org/licenses/>}.
 */

package org.dockbox.hartshorn.core.boot;

import org.dockbox.hartshorn.core.HartshornUtils;
import org.dockbox.hartshorn.core.annotations.context.LogExclude;
import org.dockbox.hartshorn.core.context.ApplicationContext;
import org.dockbox.hartshorn.core.context.ModifiableContextCarrier;
import org.dockbox.hartshorn.core.context.element.TypeContext;
import org.dockbox.hartshorn.core.domain.Exceptional;
import org.dockbox.hartshorn.core.proxy.ProxyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * The default implementation of the {@link ApplicationManager} interface. This implementation delegates most functionality
 * to concrete implementations of each of the {@link ApplicationManager} parent interfaces. If any of these implementations
 * also implement {@link ApplicationManaged}, this implementation will ensure {@link ApplicationManaged#applicationManager(ApplicationManager)}
 * is called on them.
 *
 * @author Guus Lieben
 * @since 21.9
 */
@LogExclude
@Getter
public class HartshornApplicationManager implements ObservableApplicationManager, ModifiableContextCarrier {

    private static final String BANNER = """
                 _   _            _       _                     \s
                | | | | __ _ _ __| |_ ___| |__   ___  _ __ _ __ \s
                | |_| |/ _` | '__| __/ __| '_ \\ / _ \\| '__| '_ \\\s
                |  _  | (_| | |  | |_\\__ \\ | | | (_) | |  | | | |
            ====|_| |_|\\__,_|_|===\\__|___/_|=|_|\\___/|_|==|_|=|_|====
                                             -- Hartshorn v%s --
            """.formatted(Hartshorn.VERSION);

    private final Set<LifecycleObserver> observers = HartshornUtils.emptyConcurrentSet();
    private final ApplicationFSProvider applicationFSProvider;
    private final ApplicationLogger applicationLogger;
    private final ApplicationProxier applicationProxier;
    private ApplicationContext applicationContext;

    @Setter
    private ExceptionHandler exceptionHandler;

    public HartshornApplicationManager(
            final TypeContext<?> activator,
            final ApplicationLogger applicationLogger,
            final ApplicationProxier applicationProxier,
            final ApplicationFSProvider applicationFSProvider,
            final ExceptionHandler exceptionHandler) {
        if (applicationLogger instanceof ApplicationManaged applicationManaged)
            applicationManaged.applicationManager(this);
        this.exceptionHandler = exceptionHandler;

        if (applicationLogger instanceof ApplicationManaged applicationManaged)
            applicationManaged.applicationManager(this);
        this.applicationLogger = applicationLogger;

        if (applicationProxier instanceof ApplicationManaged applicationManaged)
            applicationManaged.applicationManager(this);
        this.applicationProxier = applicationProxier;

        if (applicationFSProvider instanceof ApplicationManaged applicationManaged)
            applicationManaged.applicationManager(this);
        this.applicationFSProvider = applicationFSProvider;

        if (!this.isCI()) this.printHeader(activator);
    }

    private void printHeader(final TypeContext<?> activator) {
        final Logger logger = LoggerFactory.getLogger(activator.type());
        for (final String line : BANNER.split("\n")) {
            logger.info(line);
        }
        logger.info("");
    }

    @Override
    public boolean isCI() {
        return HartshornUtils.isCI();
    }

    @Override
    public Logger log() {
        return this.applicationLogger.log();
    }

    @Override
    public <T> Exceptional<T> proxy(final TypeContext<T> type) {
        return this.applicationProxier.proxy(type);
    }

    @Override
    public <T> Exceptional<T> proxy(final TypeContext<T> type, final T instance) {
        return this.applicationProxier.proxy(type, instance);
    }

    @Override
    public <T> Exceptional<TypeContext<T>> real(final T instance) {
        return this.applicationProxier.real(instance);
    }

    @Override
    public <T, P extends T> Exceptional<T> delegator(final TypeContext<T> type, final P instance) {
        return this.applicationProxier.delegator(type, instance);
    }

    @Override
    public <T, P extends T> Exceptional<T> delegator(final TypeContext<T> type, final ProxyHandler<P> handler) {
        return this.applicationProxier.delegator(type, handler);
    }

    @Override
    public <T> ProxyHandler<T> handler(final TypeContext<T> type, final T instance) {
        return this.applicationProxier.handler(type, instance);
    }

    @Override
    public <T> Exceptional<ProxyHandler<T>> handler(final T instance) {
        return this.applicationProxier.handler(instance);
    }

    public HartshornApplicationManager applicationContext(final ApplicationContext applicationContext) {
        if (this.applicationContext == null) this.applicationContext = applicationContext;
        else throw new IllegalArgumentException("Application context has already been configured");
        return this;
    }

    @Override
    public void register(final LifecycleObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public Path applicationPath() {
        return this.applicationFSProvider.applicationPath();
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
}
