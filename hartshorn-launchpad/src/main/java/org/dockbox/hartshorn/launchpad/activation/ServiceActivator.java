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

package org.dockbox.hartshorn.launchpad.activation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.dockbox.hartshorn.inject.InjectionCapableApplication;
import org.dockbox.hartshorn.inject.processing.ComponentPostProcessor;
import org.dockbox.hartshorn.inject.processing.ComponentPreProcessor;
import org.dockbox.hartshorn.inject.processing.HierarchicalBinderPostProcessor;
import org.dockbox.hartshorn.launchpad.condition.RequiresActivator;
import org.dockbox.hartshorn.inject.processing.ComponentProcessor;

/**
 * Annotation used to mark annotations as service activators. Service activators indicate whether
 * specific components processors become active. Additionally, they can be used to filter the activation
 * of components using the {@link RequiresActivator} annotation.
 *
 * <p>Service activators always need to be annotated with {@link ServiceActivator}. If an annotation
 * is used as activator, but is not annotated with {@link ServiceActivator}, it will be rejected by
 * the active {@link InjectionCapableApplication}.
 *
 * <p>Service activators offer a way to specify {@link #scanPackages() base packages} which become
 * active when the activator is present on the application activator. These packages will be scanned
 * when the application is initializing.
 *
 * @author Guus Lieben
 * @since 0.4.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ServiceActivator {

    /**
     * The base packages to scan for services if this service activator is present. If a package has already
     * been processed before, it is up to the active {@link InjectionCapableApplication} to decide whether to
     * process it again.
     *
     * @return The base packages to scan for services.
     */
    String[] scanPackages() default {};

    /**
     * The component processors that become active when this service activator is present.
     *
     * @return The component processors that become active when this service activator is present.
     *
     * @deprecated Use {@link #componentPreProcessors()} and {@link #componentPostProcessors()} instead for component processors,
     * and {@link #binderPostProcessors()} for binder post processors.
     */
    @Deprecated(since = "0.7.0", forRemoval = true)
    Class<? extends ComponentProcessor>[] processors() default {};

    Class<? extends ComponentPreProcessor>[] componentPreProcessors() default {};

    Class<? extends ComponentPostProcessor>[] componentPostProcessors() default {};

    Class<? extends HierarchicalBinderPostProcessor>[] binderPostProcessors() default {};
}
