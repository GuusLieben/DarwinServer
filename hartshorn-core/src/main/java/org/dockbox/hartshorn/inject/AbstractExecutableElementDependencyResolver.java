package org.dockbox.hartshorn.inject;

import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.component.ComponentContainer;
import org.dockbox.hartshorn.component.ComponentKey;
import org.dockbox.hartshorn.component.ComponentKey.Builder;
import org.dockbox.hartshorn.util.StringUtilities;
import org.dockbox.hartshorn.util.introspect.view.ExecutableElementView;
import org.dockbox.hartshorn.util.introspect.view.ParameterView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Named;

public abstract class AbstractExecutableElementDependencyResolver implements DependencyResolver {

    @Override
    public Set<DependencyContext<?>> resolve(final Collection<ComponentContainer> containers, final ApplicationContext applicationContext) throws DependencyResolutionException {
        final Set<DependencyContext<?>> dependencyContexts = new HashSet<>();
        for (final ComponentContainer componentContainer : containers) {
            dependencyContexts.addAll(this.resolveSingle(componentContainer, applicationContext));
        }
        return dependencyContexts;
    }

    protected abstract Set<DependencyContext<?>> resolveSingle(ComponentContainer componentContainer, ApplicationContext applicationContext) throws DependencyResolutionException;

    protected Set<ComponentKey<?>> resolveDependencies(final ExecutableElementView<?> bindsMethod) {
        return bindsMethod.parameters().all().stream()
                .filter(parameter -> !parameter.annotations().has(HandledInjection.class))
                .map(this::resolveComponentKey)
                .collect(Collectors.toSet());
    }

    protected <T> ComponentKey<T> resolveComponentKey(final ParameterView<T> parameter) {
        final TypeView<T> type = parameter.genericType();
        final Builder<T> keyBuilder = ComponentKey.builder(type.type());
        parameter.annotations().get(Named.class)
                .filter(qualifier -> StringUtilities.notEmpty(qualifier.value()))
                .peek(qualifier -> {
                    if (StringUtilities.notEmpty(qualifier.value())) {
                        keyBuilder.name(qualifier);
                    }
                });
        return keyBuilder.build();
    }
}
