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

package org.dockbox.hartshorn.core.context;

import org.dockbox.hartshorn.core.context.element.TypeContext;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionsPrefixContext extends AbstractPrefixContext<Reflections> {

    public ReflectionsPrefixContext(final ApplicationEnvironment environment, final Iterable<String> initialPrefixes) {
        super(environment, initialPrefixes);
    }

    @Override
    protected Reflections process(final String prefix) {
        return new Reflections(prefix);
    }

    @Override
    public <A extends Annotation> Collection<TypeContext<?>> types(final String prefix, final Class<A> annotation, final boolean skipParents) {
        final Reflections reflections = this.get(prefix);
        final Set<Class<? extends Annotation>> extensions = this.extensions(annotation);
        final Set<TypeContext<?>> types = new HashSet<>();

        for (final Class<? extends Annotation> extension : extensions) {
            for (final Class<?> type : reflections.getTypesAnnotatedWith(extension, !skipParents)) {
                types.add(TypeContext.of(type));
            }
        }
        return Set.copyOf(types);
    }

    /**
     * Gets all sub-types of a given type. The prefix is typically a package. If no sub-types exist
     * for the given type, and empty list is returned.
     *
     * @param parent The parent type to scan for subclasses
     * @param <T> The type of the parent
     *
     * @return The list of sub-types, or an empty list
     */
    @Override
    public <T> Collection<TypeContext<? extends T>> children(final TypeContext<T> parent) {
        final Set<Class<? extends T>> subTypes = new HashSet<>();
        for (final Reflections reflections : this.all()) {
            subTypes.addAll(reflections.getSubTypesOf(parent.type()));
        }
        return List.copyOf(subTypes).stream().map(TypeContext::of).collect(Collectors.toList());
    }
}
