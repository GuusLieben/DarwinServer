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

import org.dockbox.hartshorn.inject.ExceptionHandler;
import org.dockbox.hartshorn.inject.binding.HierarchicalAliasCapableBinder;
import org.dockbox.hartshorn.inject.provider.HierarchicalComponentProvider;
import org.dockbox.hartshorn.launchpad.context.ApplicationContextCarrier;
import org.dockbox.hartshorn.launchpad.environment.ApplicationEnvironment;
import org.dockbox.hartshorn.launchpad.launch.ApplicationBuilder;
import org.dockbox.hartshorn.util.ApplicationException;

/**
 * The primary context for an application. This context is responsible for providing the application
 * environment, including components, global contexts, and properties. It also provides the ability
 * to log messages and exceptions in a standardized way.
 *
 * <p>The primary context is created by the {@link ApplicationBuilder}
 * and is accessible through any {@link ApplicationContextCarrier} that is attached
 * to the application. If the context supports component injection, it can also be accessed through
 * any form of injection.
 *
 * <p>The application context is responsible for managing the lifecycle of the application, including
 * its components. The implementation may choose to perform specific actions based on available
 * activators and properties.
 *
 * @since 0.4.11
 * @author Guus Lieben
 */
public interface ApplicationContext extends
    ConfigurableActivationInjectionCapableApplication,
    HierarchicalComponentProvider,
    HierarchicalAliasCapableBinder,
    ExceptionHandler,
    AutoCloseable {

    /**
     * Gets the active {@link ApplicationEnvironment} for the application.
     *
     * @return The active {@link ApplicationEnvironment} for the application.
     * @see ApplicationEnvironment
     */
    @Override
    ApplicationEnvironment environment();

    /**
     * Indicates whether the application context is closed. A closed context cannot be used reliably to
     * access its components, as the components may have been removed from the context, or have been
     * cancelled, interrupted, or otherwise stopped.
     *
     * @return {@code true} if the context is closed, {@code false} otherwise.
     */
    boolean isClosed();

    @Override
    void close() throws ApplicationException;
}
