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

package org.dockbox.hartshorn.inject.populate;

import org.dockbox.hartshorn.inject.ComponentKey;
import org.dockbox.hartshorn.inject.ComponentKeyResolver;
import org.dockbox.hartshorn.inject.ComponentRequestContext;
import org.dockbox.hartshorn.inject.ComponentResolutionException;
import org.dockbox.hartshorn.inject.InjectionCapableApplication;
import org.dockbox.hartshorn.inject.InjectorEnvironment;
import org.dockbox.hartshorn.inject.annotations.Inject;
import org.dockbox.hartshorn.inject.provider.ComponentProvider;
import org.dockbox.hartshorn.inject.targets.AnnotatedInjectionPointRequireRule;
import org.dockbox.hartshorn.inject.targets.ComponentInjectionPoint;
import org.dockbox.hartshorn.inject.targets.ComponentInjectionPointsResolver;
import org.dockbox.hartshorn.inject.targets.InjectionPoint;
import org.dockbox.hartshorn.inject.targets.RequireInjectionPointRule;
import org.dockbox.hartshorn.util.ContextualInitializer;
import org.dockbox.hartshorn.util.Customizer;
import org.dockbox.hartshorn.util.LazyStreamableConfigurer;
import org.dockbox.hartshorn.util.StreamableConfigurer;
import org.dockbox.hartshorn.util.introspect.convert.ConversionService;

import java.util.Set;

/**
 * A {@link ComponentPopulationStrategy} which populates components with other components. This provides basic support for
 * {@link Inject} annotated fields, or any other annotation which is configured to be used for injection.
 *
 * <p>Injected components will be resolved by their {@link ComponentKey}, which is determined by the provided {@link
 * ComponentKeyResolver}.
 *
 * <p>By default, all components are resolved through the configured {@link ComponentProvider}. Additional {@link InjectParameterResolver}
 * implementations can be registered to provide custom resolution logic for specific injection points. This is primarily useful
 * for injecting components which are not registered in the {@link ComponentProvider}, or cannot be resolved through the
 * {@link ComponentProvider} alone. Built-in support for {@link org.dockbox.hartshorn.context.ContextView} types is provided by the
 * {@link InjectContextParameterResolver}.
 *
 * <p>Example:
 * <pre>{@code
 * @Component
 * public class MyComponent {
 *
 *    @Inject
 *    private MyOtherComponent otherComponent;
 *
 *    @Inject
 *    public void doSomething(SampleContext context) { ... }
 * }
 * }</pre>
 *
 * @see Inject
 * @see ComponentKey
 * @see InjectParameterResolver
 *
 * @since 0.6.0
 *
 * @author Guus Lieben
 */
public class InjectPopulationStrategy extends AbstractComponentPopulationStrategy {

    private final ComponentKeyResolver componentKeyResolver;
    private final ComponentInjectionPointsResolver injectionPointsResolver;
    private final ComponentProvider componentProvider;
    private final Set<InjectParameterResolver> parameterResolvers;
    private ConversionService conversionService;

    protected InjectPopulationStrategy(
            ComponentKeyResolver componentKeyResolver,
            ComponentInjectionPointsResolver injectionPointsResolver,
            ComponentProvider componentProvider,
            Set<RequireInjectionPointRule> requiresComponentRules,
            Set<InjectParameterResolver> parameterResolvers) {
        super(requiresComponentRules);
        this.componentKeyResolver = componentKeyResolver;
        this.injectionPointsResolver = injectionPointsResolver;
        this.componentProvider = componentProvider;
        this.parameterResolvers = parameterResolvers;
    }

    @Override
    protected boolean isApplicable(ComponentInjectionPoint<?> injectionPoint) {
        return this.injectionPointsResolver.isInjectable(injectionPoint.declaration());
    }

    @Override
    protected Object resolveInjectedObject(InjectionPoint injectionPoint, PopulateComponentContext<?> context) throws ComponentResolutionException {
        for(InjectParameterResolver resolver : this.parameterResolvers) {
            if (resolver.accepts(injectionPoint)) {
                Object resolved = resolver.resolve(injectionPoint, context);
                // Parameter resolvers are expected to provide compatible instances, or null if they cannot resolve the injection point.
                // If a non-null value is provided, it must be compatible with the injection point type. If it is not, we do not want
                // to attempt a manual conversion through the ConversionService, as we cannot make assumptions about custom implementations,
                // and thus risk resulting in unexpected behaviour.
                if (resolved == null || injectionPoint.type().isInstance(resolved)) {
                    return resolved;
                }
                else {
                    throw new ComponentResolutionException("Failed to resolve injection point " + injectionPoint.injectionPoint().qualifiedName() + ", expected type " + injectionPoint.type().type().getName() + " but got " + resolved.getClass().getName(), null);
                }
            }
        }

        ComponentKey<?> componentKey = this.componentKeyResolver.resolve(injectionPoint.injectionPoint());
        ComponentRequestContext requestContext = ComponentRequestContext.createForInjectionPoint(injectionPoint);
        Object component = this.componentProvider.get(componentKey, requestContext);

        // Ensure types are compatible, or a default value is provided if it is available. This primarily
        // applies to component collections.
        return this.conversionService().convert(component, injectionPoint.type().type());
    }

    private ConversionService conversionService() {
        if (null == this.conversionService) {
            this.conversionService = this.componentProvider.get(ConversionService.class);
        }
        return this.conversionService;
    }

    public static ContextualInitializer<InjectionCapableApplication, ComponentPopulationStrategy> create(Customizer<Configurer> customizer) {
        return context -> {
            Configurer configurer = new Configurer();
            customizer.configure(configurer);
            InjectionCapableApplication application = context.input();
            InjectorEnvironment environment = application.environment();
            return new InjectPopulationStrategy(
                    environment.componentKeyResolver(),
                    environment.injectionPointsResolver(),
                    application.defaultProvider(),
                    Set.copyOf(configurer.requiresComponentRules.initialize(context)),
                    Set.copyOf(configurer.parameterResolvers.initialize(context))
            );
        };
    }

    /**
     * Configurer for the {@link InjectPopulationStrategy}, that allows for the configuration of rules to determine
     * how additional injection parameter values are resolved, and whether a specific injection point is required
     * to be resolved for a component to be considered valid.
     *
     * @since 0.6.0
     *
     * @author Guus Lieben
     */
    public static class Configurer {

        private final LazyStreamableConfigurer<InjectionCapableApplication, RequireInjectionPointRule> requiresComponentRules = LazyStreamableConfigurer.of(new AnnotatedInjectionPointRequireRule());
        private final LazyStreamableConfigurer<InjectionCapableApplication, InjectParameterResolver> parameterResolvers = LazyStreamableConfigurer.of(configurer -> {
            configurer.add(ContextualInitializer.of(InjectContextParameterResolver::new));
            configurer.add(ContextualInitializer.of(application -> new InjectPropertyParameterResolver(application.defaultProvider())));
        });

        public Configurer requiresComponentRules(RequireInjectionPointRule... requiresComponentRules) {
            this.requiresComponentRules.customizer(collection -> collection.addAll(requiresComponentRules));
            return this;
        }

        public Configurer requiresComponentRules(Set<RequireInjectionPointRule> requiresComponentRules) {
            this.requiresComponentRules.customizer(collection -> collection.addAll(requiresComponentRules));
            return this;
        }

        public Configurer requiresComponentRules(Customizer<StreamableConfigurer<InjectionCapableApplication, RequireInjectionPointRule>> customizer) {
            this.requiresComponentRules.customizer(customizer);
            return this;
        }

        public Configurer parameterResolvers(InjectParameterResolver... parameterResolvers) {
            this.parameterResolvers.customizer(collection -> collection.addAll(parameterResolvers));
            return this;
        }

        public Configurer parameterResolvers(Set<InjectParameterResolver> parameterResolvers) {
            this.parameterResolvers.customizer(collection -> collection.addAll(parameterResolvers));
            return this;
        }

        public Configurer parameterResolvers(Customizer<StreamableConfigurer<InjectionCapableApplication, InjectParameterResolver>> customizer) {
            this.parameterResolvers.customizer(customizer);
            return this;
        }

    }
}
