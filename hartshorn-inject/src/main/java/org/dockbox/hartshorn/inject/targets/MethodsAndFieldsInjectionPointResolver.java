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

package org.dockbox.hartshorn.inject.targets;

import org.dockbox.hartshorn.inject.InjectorEnvironment;
import org.dockbox.hartshorn.inject.annotations.Inject;
import org.dockbox.hartshorn.inject.annotations.Populate;
import org.dockbox.hartshorn.util.ContextualInitializer;
import org.dockbox.hartshorn.util.Customizer;
import org.dockbox.hartshorn.util.LazyStreamableConfigurer;
import org.dockbox.hartshorn.util.StreamableConfigurer;
import org.dockbox.hartshorn.util.TypeUtils;
import org.dockbox.hartshorn.util.introspect.view.AnnotatedGenericTypeView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link ComponentInjectionPointsResolver} that resolves the injection points of a component by
 * inspecting the methods and fields of the component. The injection points are resolved based on
 * the {@link Populate} annotation on the component type. If no {@link Populate} annotation is
 * present, all injection points will be resolved.
 *
 * @see Populate
 * @see ComponentInjectionPoint
 *
 * @since 0.6.0
 *
 * @author Guus Lieben
 */
public class MethodsAndFieldsInjectionPointResolver implements ComponentInjectionPointsResolver {

    private final Set<Class<? extends Annotation>> injectAnnotations;

    public MethodsAndFieldsInjectionPointResolver(Set<Class<? extends Annotation>> injectAnnotations) {
        this.injectAnnotations = injectAnnotations;
    }

    @Override
    public <T> Set<ComponentInjectionPoint<T>> resolve(TypeView<T> type) {
        Set<Populate.Type> types = type.annotations().get(Populate.class)
                .map(Populate::value)
                .map(value -> EnumSet.copyOf(Set.of(value)))
                .orElseGet(() -> EnumSet.allOf(Populate.Type.class));

        Set<ComponentInjectionPoint<T>> injectionPoints = new HashSet<>();
        if (types.contains(Populate.Type.EXECUTABLES)) {
            type.methods().all().stream()
                    .filter(this::isInjectable)
                    .map(ComponentMethodInjectionPoint::new)
                    .forEach(injectionPoints::add);
        }
        if (types.contains(Populate.Type.FIELDS)) {
            type.fields().all().stream()
                    .filter(this::isInjectable)
                    .map(ComponentFieldInjectionPoint::new)
                    .forEach(injectionPoints::add);
        }
        return injectionPoints;
    }

    @Override
    public boolean isInjectable(AnnotatedGenericTypeView<?> declaration) {
        return declaration.annotations().hasAny(this.injectAnnotations);
    }

    public static ContextualInitializer<InjectorEnvironment, ComponentInjectionPointsResolver> create(Customizer<Configurer> customizer) {
        return context -> {
            Configurer configurer = new Configurer();
            customizer.configure(configurer);

            List<Class<? extends Annotation>> annotationTypes = configurer.annotations.initialize(context);
            return new MethodsAndFieldsInjectionPointResolver(Set.copyOf(annotationTypes));
        };
    }

    /**
     * A configurer for the {@link MethodsAndFieldsInjectionPointResolver}, that allows for the configuration of
     * supported annotations.
     *
     * @since 0.6.0
     *
     * @author Guus Lieben
     */
    public static class Configurer {

        private final LazyStreamableConfigurer<InjectorEnvironment, Class<? extends Annotation>> annotations = LazyStreamableConfigurer.of(Inject.class);

        /**
         * Configures the annotations to be used for injection points. By default, this only contains {@link Inject}.
         *
         * @param annotations The annotations to use for injection points
         * @return The current configurer, for chaining
         */
        @SafeVarargs
        public final Configurer annotations(Class<? extends Annotation>... annotations) {
            this.annotations(collection -> collection.addAll(annotations));
            return this;
        }

        /**
         * Configures the annotations to be used for injection points. By default, this only contains {@link Inject}.
         *
         * @param annotations The annotations to use for injection points
         * @return The current configurer, for chaining
         */
        public Configurer annotations(Set<Class<? extends Annotation>> annotations) {
            this.annotations(collection -> collection.addAll(annotations));
            return this;
        }

        /**
         * Configures the annotations to be used for injection points. By default, this only contains {@link Inject}.
         *
         * @param customizer The customizer to configure the annotations
         * @return The current configurer, for chaining
         */
        public Configurer annotations(Customizer<StreamableConfigurer<InjectorEnvironment, Class<? extends Annotation>>> customizer) {
            this.annotations.customizer(customizer);
            return this;
        }

        /**
         * Adds support for {@code javax.inject.Inject} and {@code javax.annotation.Resource} annotations if they are present
         * on the classpath. Disabled by default, but can be enabled for backwards compatibility.
         *
         * @return the current configurer, for chaining
         */
        public Configurer withJavaxAnnotations() {
            return this.annotations(collection -> {
                TypeUtils.<Annotation>forName("javax.inject.Inject", Annotation.class).peek(collection::add);
                TypeUtils.<Annotation>forName("javax.annotation.Resource", Annotation.class).peek(collection::add);
            });
        }

        /**
         * Adds support for {@code jakarta.inject.Inject} and {@code jakarta.annotation.Resource} annotations if they are present
         * on the classpath. Disabled by default, but can be enabled for backwards compatibility.
         *
         * @return the current configurer, for chaining
         */
        public Configurer withJakartaAnnotations() {
            return this.annotations(collection -> {
                TypeUtils.<Annotation>forName("jakarta.inject.Inject", Annotation.class).peek(collection::add);
                TypeUtils.<Annotation>forName("jakarta.annotation.Resource", Annotation.class).peek(collection::add);
            });
        }

    }
}
